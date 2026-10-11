package com.ninemensmorris.game.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

// 스프링 없이 도는 단위 테스트용 저장소. 계약은 RedisRoomRegistry 와 같음(RoomRegistryContractTest)
// 운영 저장소와 달리 같은 Room 객체를 계속 쥐므로 테스트가 방을 들고 상태를 볼 수 있음
public class InMemoryRoomRegistry implements RoomRegistry {

    private final ConcurrentMap<Long, Room> rooms = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public Room create(String title, long hostId) {
        long roomId = sequence.incrementAndGet();
        Room room = new Room(roomId, title, hostId);
        rooms.put(roomId, room);
        return room;
    }

    @Override
    public Optional<Room> find(long roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    @Override
    public List<Room> findAll() {
        return rooms.values().stream()
                .sorted(Comparator.comparing(Room::createdAt).reversed())
                .toList();
    }

    @Override
    public int size() {
        return rooms.size();
    }

    @Override
    public <R> Optional<R> mutate(long roomId, Function<Room, R> action) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return Optional.empty();
        }
        synchronized (room) {
            R result = action.apply(room);
            if (room.isClosed()) {
                rooms.remove(roomId);
            } else {
                room.touch();
            }
            return Optional.ofNullable(result);
        }
    }

    @Override
    public List<Long> findTurnExpired(Instant now) {
        return rooms.values().stream()
                .filter(room -> room.isTurnExpired(now))
                .map(Room::roomId)
                .toList();
    }

    @Override
    public List<Long> findIdle(Instant threshold) {
        return rooms.values().stream()
                .filter(room -> room.lastActivityAt().isBefore(threshold))
                .map(Room::roomId)
                .toList();
    }
}
