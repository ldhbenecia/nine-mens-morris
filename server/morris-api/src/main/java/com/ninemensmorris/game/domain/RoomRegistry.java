package com.ninemensmorris.game.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

// 방 저장소
//
// 방 하나에 대한 변경은 mutate 하나로만 일어나고 방 단위로 직렬화됨. 방끼리는 병렬
// 여러 서버가 같은 저장소를 쓰므로 find / findAll 이 주는 Room 은 그 순간의 사본임
public interface RoomRegistry {

    Room create(String title, long hostId);

    Optional<Room> find(long roomId);

    // 최근에 만든 방부터
    List<Room> findAll();

    // 게이지와 헬스체크가 주기적으로 읽음. 목록을 만들지 않고 셈
    int size();

    // 방 하나에 대한 모든 변경은 여기를 거침. 방이 없으면 empty
    // action 이 room.close() 를 부르면 저장 대신 지움
    // action 안에서 저장소를 직접 부르지 않음. 같은 방의 락을 다시 잡으려다 멈춤
    <R> Optional<R> mutate(long roomId, Function<Room, R> action);

    // 아래 둘은 락 없이 고른 후보. 고른 뒤 mutate 안에서 다시 확인해야 함

    List<Long> findTurnExpired(Instant now);

    // 주어진 시각보다 오래 방치된 방. 얼마나 방치돼야 정리 대상인지는 호출자가 정함
    List<Long> findIdle(Instant threshold);
}
