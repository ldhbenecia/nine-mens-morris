package com.ninemensmorris.game.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.springframework.stereotype.Component;

// 방 저장소
//
// 기존에는 gameId 하나의 상태가 동기화 없는 HashMap 11개에 흩어져 있었다
// STOMP 메시지는 스레드 풀에서 병렬 처리되므로 락 없이 접근하면 값이 유실된다
// 방 하나당 락을 걸어 직렬화하고, 방끼리는 그대로 병렬로 처리한다
@Component
public class RoomRegistry {

    private final ConcurrentMap<Long, Room> rooms = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public Room create(String title, long hostId) {
        long roomId = sequence.incrementAndGet();
        Room room = new Room(roomId, title, hostId);
        rooms.put(roomId, room);
        return room;
    }

    public Optional<Room> find(long roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    public List<Room> findAll() {
        return rooms.values().stream()
                .sorted(Comparator.comparing(Room::createdAt).reversed())
                .toList();
    }

    // 사용자가 참여 중인 방. 소켓이 끊겼을 때 찾는 용도
    public Optional<Room> findByPlayer(long userId) {
        return rooms.values().stream().filter(room -> room.contains(userId)).findFirst();
    }

    public void remove(long roomId) {
        rooms.remove(roomId);
    }

    public int size() {
        return rooms.size();
    }

    // 방 하나에 대한 모든 변경은 이 메서드를 거친다
    public <R> Optional<R> mutate(long roomId, Function<Room, R> action) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return Optional.empty();
        }
        synchronized (room) {
            R result = action.apply(room);
            room.touch();
            return Optional.ofNullable(result);
        }
    }

    // 주어진 시각보다 오래 방치된 방. 얼마나 방치돼야 정리 대상인지는 호출자가 정함
    // 여기서 바로 지우지 않는 이유는 진행 중인 판을 정산할 수 있는 곳이 아니어서다
    public List<Long> findIdle(Instant threshold) {
        return rooms.values().stream()
                .filter(room -> room.lastActivityAt().isBefore(threshold))
                .map(Room::roomId)
                .toList();
    }
}
