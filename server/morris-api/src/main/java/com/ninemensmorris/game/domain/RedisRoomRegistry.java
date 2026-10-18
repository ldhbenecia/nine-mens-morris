package com.ninemensmorris.game.domain;

import com.ninemensmorris.game.repository.RoomStore;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

// 방 저장소의 운영 구현. 저장·락·색인은 RoomStore(Redis) 가 하고 여기서는 스냅샷과 Room 을 오감
// 저장소는 방의 규칙을 모르고, 규칙은 Room 이 앎
@Component
public class RedisRoomRegistry implements RoomRegistry {

    private final RoomStore store;
    private final ApplicationEventPublisher events;

    public RedisRoomRegistry(RoomStore store, ApplicationEventPublisher events) {
        this.store = store;
        this.events = events;
    }

    @Override
    public Room create(String title, long hostId) {
        Room room = new Room(store.nextId(), title, hostId);
        store.insert(room.snapshot());
        events.publishEvent(new LobbyChanged(room.roomId()));
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

    // 로비 알림은 락을 푼 뒤에 냄. 락 안에서 보내면 그동안 그 방의 다음 수가 기다림
    @Override
    public <R> Optional<R> mutate(long roomId, Function<Room, R> action) {
        Optional<Mutated<R>> mutated = store.update(roomId, snapshot -> {
            Room room = Room.restore(snapshot);
            room.nextRevision();
            RoomSnapshot unchanged = room.snapshot();
            Room.LobbyView listed = room.lobbyView();
            R result = action.apply(room);
            if (room.isClosed()) {
                return RoomStore.Change.delete(new Mutated<>(result, true));
            }
            // 거절된 수·남의 방 나가기처럼 바뀐 게 없으면 다시 쓰지 않음. 올린 revision 도 저장되지 않아 그대로
            if (room.snapshot().equals(unchanged)) {
                return RoomStore.Change.keep(new Mutated<>(result, false));
            }
            room.touch();
            return RoomStore.Change.save(
                    room.snapshot(), new Mutated<>(result, !room.lobbyView().equals(listed)));
        });
        mutated.filter(Mutated::lobbyChanged).ifPresent(changed -> events.publishEvent(new LobbyChanged(roomId)));
        return mutated.map(Mutated::value);
    }

    private record Mutated<R>(R value, boolean lobbyChanged) {}

    @Override
    public List<Long> findTurnExpired(Instant now) {
        return store.findTurnExpired(now);
    }

    @Override
    public List<Long> findIdle(Instant threshold) {
        return store.findIdle(threshold);
    }
}
