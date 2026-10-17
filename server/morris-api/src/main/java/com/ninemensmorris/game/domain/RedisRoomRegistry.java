package com.ninemensmorris.game.domain;

import com.ninemensmorris.game.repository.RoomStore;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Component;

// 방 저장소의 운영 구현. 저장·락·색인은 RoomStore(Redis) 가 하고 여기서는 스냅샷과 Room 을 오감
// 저장소는 방의 규칙을 모르고, 규칙은 Room 이 앎
@Component
public class RedisRoomRegistry implements RoomRegistry {

    private final RoomStore store;

    public RedisRoomRegistry(RoomStore store) {
        this.store = store;
    }

    @Override
    public Room create(String title, long hostId) {
        Room room = new Room(store.nextId(), title, hostId);
        store.insert(room.snapshot());
        return room;
    }

    @Override
    public Optional<Room> find(long roomId) {
        return store.find(roomId).map(Room::restore);
    }

    @Override
    public List<Room> findAll() {
        return store.findAll().stream()
                .map(Room::restore)
                .sorted(Comparator.comparing(Room::createdAt).reversed())
                .toList();
    }

    @Override
    public int size() {
        return store.size();
    }

    @Override
    public <R> Optional<R> mutate(long roomId, Function<Room, R> action) {
        return store.update(roomId, snapshot -> {
            Room room = Room.restore(snapshot);
            R result = action.apply(room);
            if (room.isClosed()) {
                return RoomStore.Change.delete(result);
            }
            // 거절된 수·동기화 읽기·남의 방 나가기처럼 바뀐 게 없으면 다시 쓰지 않음. 활동 시각·revision 도 그대로
            if (room.snapshot().equals(snapshot)) {
                return RoomStore.Change.keep(result);
            }
            room.touch();
            room.nextRevision();
            return RoomStore.Change.save(room.snapshot(), result);
        });
    }

    @Override
    public List<Long> findTurnExpired(Instant now) {
        return store.findTurnExpired(now);
    }

    @Override
    public List<Long> findIdle(Instant threshold) {
        return store.findIdle(threshold);
    }
}
