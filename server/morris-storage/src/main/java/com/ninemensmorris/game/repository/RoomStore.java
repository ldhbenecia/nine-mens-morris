package com.ninemensmorris.game.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ninemensmorris.core.game.GameStatus;
import com.ninemensmorris.game.domain.RoomSnapshot;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

// 방 상태를 Redis 에 둠. 여러 서버가 같은 저장소를 보고 어느 서버든 어느 방이든 처리함
// 방의 규칙은 모름. 스냅샷(데이터)을 넣고 꺼내고, 한 방의 변경을 한 번에 하나씩만 일어나게 함
//
// 키
//   room:{id}             해시 { json, ver }
//   rooms                 방 id 집합
//   rooms:turn-deadline   진행 중인 방의 마감 시각(ms) — 턴 만료 후보
//   rooms:activity        마지막 활동 시각(ms) — 유휴 정리 후보
//   rooms:seq             방 번호
//   lock:room:{id}        방 락
//
// 변경은 "락 → 읽기 → 고치기 → 버전 확인 후 쓰기 → 락 해제"
// 락만으로는 GC 멈춤 등으로 임대가 끝난 뒤 늦게 도착한 쓰기를 막지 못해 쓰기에서 버전을 한 번 더 봄
// 쓰기와 색인 갱신을 한 Lua 로 묶어 중간에 서버가 죽어도 방과 색인이 어긋나지 않음
// 스크립트가 여러 키를 건드리므로 Redis Cluster 가 아니라 단일 마스터(Sentinel)를 전제로 함
@Repository
public class RoomStore {

    private static final String ROOMS = "rooms";
    private static final String TURN_DEADLINES = "rooms:turn-deadline";
    private static final String ACTIVITY = "rooms:activity";
    private static final String SEQUENCE = "rooms:seq";
    private static final String JSON = "json";

    // 변경 하나는 수 ms. 넉넉히 잡아도 서버가 죽었을 때 그 방이 이만큼만 멈춤
    private static final Duration LOCK_LEASE = Duration.ofSeconds(5);
    private static final Duration LOCK_WAIT = Duration.ofSeconds(3);
    private static final long LOCK_RETRY_MILLIS = 10;

    private static final RedisScript<Long> SAVE = RedisScript.of(
            """
            local current = tonumber(redis.call('HGET', KEYS[1], 'ver') or '0')
            if current ~= tonumber(ARGV[1]) then return 0 end
            redis.call('HSET', KEYS[1], 'json', ARGV[2], 'ver', current + 1)
            redis.call('SADD', KEYS[2], ARGV[3])
            if ARGV[4] == '' then
              redis.call('ZREM', KEYS[3], ARGV[3])
            else
              redis.call('ZADD', KEYS[3], ARGV[4], ARGV[3])
            end
            redis.call('ZADD', KEYS[4], ARGV[5], ARGV[3])
            return 1
            """,
            Long.class);

    private static final RedisScript<Long> DELETE = RedisScript.of(
            """
            local current = tonumber(redis.call('HGET', KEYS[1], 'ver') or '0')
            if current ~= tonumber(ARGV[1]) then return 0 end
            redis.call('DEL', KEYS[1])
            redis.call('SREM', KEYS[2], ARGV[2])
            redis.call('ZREM', KEYS[3], ARGV[2])
            redis.call('ZREM', KEYS[4], ARGV[2])
            return 1
            """,
            Long.class);

    // 자기 토큰일 때만 지움. 임대가 끝나 다른 서버가 잡은 락을 지우지 않게
    private static final RedisScript<Long> UNLOCK = RedisScript.of(
            """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
              return redis.call('DEL', KEYS[1])
            end
            return 0
            """,
            Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RoomStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public long nextId() {
        return Objects.requireNonNull(redis.opsForValue().increment(SEQUENCE));
    }

    public void insert(RoomSnapshot room) {
        if (!save(room, 0)) {
            throw new IllegalStateException("이미 있는 방 번호 roomId=" + room.roomId());
        }
    }

    public Optional<RoomSnapshot> find(long roomId) {
        Object json = redis.opsForHash().get(roomKey(roomId), JSON);
        return Optional.ofNullable(json).map(value -> read((String) value));
    }

    // 방마다 왕복하면 로비 요청 하나가 방 수만큼 왕복함. 파이프라인으로 한 번에
    public List<RoomSnapshot> findAll() {
        Set<String> ids = redis.opsForSet().members(ROOMS);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<String> keys = ids.stream().map(id -> roomKey(Long.parseLong(id))).toList();
        List<Object> jsons = redis.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : keys) {
                connection
                        .hashCommands()
                        .hGet(key.getBytes(StandardCharsets.UTF_8), JSON.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });
        // 목록을 뜬 사이 지워진 방은 null
        return jsons.stream()
                .filter(Objects::nonNull)
                .map(json -> read((String) json))
                .toList();
    }

    public int size() {
        Long size = redis.opsForSet().size(ROOMS);
        return size == null ? 0 : size.intValue();
    }

    // 한 방의 변경. 락을 쥔 채로 change 를 한 번만 부름. 방이 없으면 empty
    // change 는 다음 스냅샷과 결과를 돌려줌. 다음 스냅샷이 null 이면 방을 지움
    public <R> Optional<R> update(long roomId, Function<RoomSnapshot, Change<R>> change) {
        String lockKey = "lock:room:" + roomId;
        String token = UUID.randomUUID().toString();
        acquire(lockKey, token, roomId);
        try {
            Map<Object, Object> stored = redis.opsForHash().entries(roomKey(roomId));
            if (stored.isEmpty()) {
                return Optional.empty();
            }
            long version = Long.parseLong((String) stored.get("ver"));
            Change<R> result = change.apply(read((String) stored.get(JSON)));

            boolean written = result.next() == null ? delete(roomId, version) : save(result.next(), version);
            if (!written) {
                // 락을 쥔 채로는 일어나지 않음. 임대가 끝난 뒤에 쓰려 한 것
                throw new IllegalStateException("방 락 임대가 끝난 뒤 쓰기 시도 roomId=" + roomId);
            }
            return Optional.ofNullable(result.value());
        } finally {
            redis.execute(UNLOCK, List.of(lockKey), token);
        }
    }

    public List<Long> findTurnExpired(Instant now) {
        return idsByScore(TURN_DEADLINES, now.toEpochMilli());
    }

    public List<Long> findIdle(Instant threshold) {
        return idsByScore(ACTIVITY, threshold.toEpochMilli() - 1);
    }

    // next 가 null 이면 지움
    public record Change<R>(RoomSnapshot next, R value) {

        public static <R> Change<R> save(RoomSnapshot next, R value) {
            return new Change<>(next, value);
        }

        public static <R> Change<R> delete(R value) {
            return new Change<>(null, value);
        }
    }

    private void acquire(String lockKey, String token, long roomId) {
        long deadline = System.nanoTime() + LOCK_WAIT.toNanos();
        while (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, token, LOCK_LEASE))) {
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException("방 락을 얻지 못함 roomId=" + roomId);
            }
            try {
                Thread.sleep(LOCK_RETRY_MILLIS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("방 락 대기 중 중단 roomId=" + roomId, interrupted);
            }
        }
    }

    private boolean save(RoomSnapshot room, long expectedVersion) {
        long roomId = room.roomId();
        Instant deadline = isPlaying(room) ? room.turnDeadline() : null;
        Long result = redis.execute(
                SAVE,
                List.of(roomKey(roomId), ROOMS, TURN_DEADLINES, ACTIVITY),
                String.valueOf(expectedVersion),
                write(room),
                String.valueOf(roomId),
                deadline == null ? "" : String.valueOf(deadline.toEpochMilli()),
                String.valueOf(room.lastActivityAt().toEpochMilli()));
        return Long.valueOf(1).equals(result);
    }

    private boolean delete(long roomId, long expectedVersion) {
        Long result = redis.execute(
                DELETE,
                List.of(roomKey(roomId), ROOMS, TURN_DEADLINES, ACTIVITY),
                String.valueOf(expectedVersion),
                String.valueOf(roomId));
        return Long.valueOf(1).equals(result);
    }

    // 끝난 판은 마감이 남아 있어도 턴 만료 후보가 아님
    private static boolean isPlaying(RoomSnapshot room) {
        return room.game() != null && room.game().status() == GameStatus.PLAYING;
    }

    private List<Long> idsByScore(String key, long maxScore) {
        Set<String> ids = redis.opsForZSet().rangeByScore(key, Double.NEGATIVE_INFINITY, maxScore);
        if (ids == null) {
            return List.of();
        }
        return ids.stream().map(Long::parseLong).toList();
    }

    private static String roomKey(long roomId) {
        return "room:" + roomId;
    }

    private String write(RoomSnapshot room) {
        try {
            return objectMapper.writeValueAsString(room);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("방 직렬화 실패 roomId=" + room.roomId(), exception);
        }
    }

    private RoomSnapshot read(String json) {
        try {
            return objectMapper.readValue(json, RoomSnapshot.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("방 역직렬화 실패", exception);
        }
    }
}
