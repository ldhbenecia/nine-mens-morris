package com.ninemensmorris.game.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.ninemensmorris.core.board.Stone;
import com.ninemensmorris.core.move.Move;
import com.ninemensmorris.core.move.MoveResult;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 방 저장소가 지켜야 할 약속. 운영(Redis)과 단위 테스트용(메모리) 구현이 같이 통과해야 함
//
// otherServer 는 같은 저장소를 보는 다른 서버. 메모리 구현은 자기 자신을 돌려줌
// 인터페이스인 이유: Redis 쪽 테스트가 스프링 통합 테스트 지원을 상속해야 해서 클래스로는 둘 다 받을 수 없음
interface RoomRegistryContract {

    long HOST = 1L;
    long GUEST = 2L;

    RoomRegistry registry();

    RoomRegistry otherServer();

    private long 시작된_방() {
        long roomId = registry().create("방", HOST).roomId();
        registry().mutate(roomId, room -> {
            room.join(GUEST);
            room.changeFirstMoveRule(FirstMoveRule.HOST_FIRST);
            room.start();
            return null;
        });
        return roomId;
    }

    @Test
    @DisplayName("만든 방을 찾을 수 있고 목록은 최근에 만든 방부터다")
    default void 만든_방을_찾는다() throws InterruptedException {
        // given
        long first = registry().create("먼저", HOST).roomId();
        Thread.sleep(5); // 만든 시각으로 정렬하므로 같은 ms 를 피함
        long second = registry().create("나중", HOST).roomId();

        // then
        assertThat(otherServer().find(first)).get().extracting(Room::title).isEqualTo("먼저");
        assertThat(otherServer().findAll()).extracting(Room::roomId).containsExactly(second, first);
        assertThat(otherServer().size()).isEqualTo(2);
    }

    @Test
    @DisplayName("한 서버에서 고친 방을 다른 서버가 그대로 본다")
    default void 변경이_다른_서버에_보인다() {
        // given
        long roomId = registry().create("방", HOST).roomId();

        // when
        registry().mutate(roomId, room -> {
            room.join(GUEST);
            return null;
        });

        // then
        assertThat(otherServer().find(roomId)).get().extracting(Room::guestId).isEqualTo(GUEST);
    }

    @Test
    @DisplayName("판이 서버를 건너 이어진다")
    default void 판이_서버를_건너_이어진다() {
        // given — 흑(방장)이 이 서버에서 둠
        long roomId = 시작된_방();
        registry().mutate(roomId, room -> room.game().apply(Stone.BLACK, new Move.Place(0)));

        // when — 백(참가자)이 다른 서버에서 둠
        MoveResult result = otherServer()
                .mutate(roomId, room -> room.game().apply(Stone.WHITE, new Move.Place(9)))
                .orElseThrow();

        // then
        assertThat(result).isInstanceOf(MoveResult.Applied.class);
        Room room = registry().find(roomId).orElseThrow();
        assertThat(room.game().board().stoneAt(0)).isEqualTo(Stone.BLACK);
        assertThat(room.game().board().stoneAt(9)).isEqualTo(Stone.WHITE);
        assertThat(room.game().currentTurn()).isEqualTo(Stone.BLACK);
    }

    @Test
    @DisplayName("변경 중에 닫은 방은 목록에서도 후보에서도 사라진다")
    default void 닫은_방은_사라진다() {
        // given
        long roomId = 시작된_방();
        registry().mutate(roomId, room -> {
            room.restartTurnClock(Instant.now());
            return null;
        });

        // when
        registry().mutate(roomId, room -> {
            room.close();
            return null;
        });

        // then
        Instant later = Instant.now().plus(Duration.ofHours(1));
        assertThat(otherServer().find(roomId)).isEmpty();
        assertThat(otherServer().findAll()).isEmpty();
        assertThat(otherServer().size()).isZero();
        assertThat(otherServer().findTurnExpired(later)).isEmpty();
        assertThat(otherServer().findIdle(later)).isEmpty();
    }

    @Test
    @DisplayName("고친 방은 revision 과 활동 시각이 오른다")
    default void 고치면_revision_이_오른다() throws InterruptedException {
        // given — 메모리 구현은 같은 객체를 돌려주므로 값을 먼저 떠 둠
        Room created = registry().create("방", HOST);
        long revision = created.revision();
        Instant activity = created.lastActivityAt();
        Thread.sleep(5); // 활동 시각 비교라 같은 ms 를 피함

        // when
        registry().mutate(created.roomId(), room -> {
            room.join(GUEST);
            return null;
        });

        // then
        Room room = otherServer().find(created.roomId()).orElseThrow();
        assertThat(room.revision()).isEqualTo(revision + 1);
        assertThat(room.lastActivityAt()).isAfter(activity);
    }

    @Test
    @DisplayName("변경 안에서 본 revision 이 저장되는 revision 이다")
    default void 변경_안의_revision_이_저장된다() {
        // given
        long roomId = registry().create("방", HOST).roomId();

        // when — 이벤트의 판 상태는 변경 안에서 만들어짐
        long seenInside = registry()
                .mutate(roomId, room -> {
                    room.join(GUEST);
                    return room.revision();
                })
                .orElseThrow();

        // then — 다르면 다음 sync 와 다음 수의 이벤트가 같은 번호를 달아 화면이 한 수 전으로 되돌아갈 수 있음
        assertThat(otherServer().find(roomId).orElseThrow().revision()).isEqualTo(seenInside);
    }

    @Test
    @DisplayName("아무것도 바꾸지 않은 변경은 revision 도 활동 시각도 그대로다")
    default void 바뀐_게_없으면_그대로다() throws InterruptedException {
        // given — 거절된 수. 판은 그대로
        long roomId = 시작된_방();
        Room before = otherServer().find(roomId).orElseThrow();
        long revision = before.revision();
        Instant activity = before.lastActivityAt();
        Thread.sleep(5);

        // when
        MoveResult result = registry()
                .mutate(roomId, room -> room.game().apply(Stone.WHITE, new Move.Place(0)))
                .orElseThrow();

        // then — 다른 서버가 보낸 이벤트의 순서 판단과 유휴 정리가 헛된 요청에 흔들리지 않음
        assertThat(result).isInstanceOf(MoveResult.Rejected.class);
        Room after = otherServer().find(roomId).orElseThrow();
        assertThat(after.revision()).isEqualTo(revision);
        assertThat(after.lastActivityAt()).isEqualTo(activity);
    }

    @Test
    @DisplayName("쓰지 않고 끝난 변경도 락을 풀어 다음 변경이 기다리지 않는다")
    default void 바뀐_게_없어도_락을_푼다() {
        // given
        long roomId = registry().create("방", HOST).roomId();
        registry().mutate(roomId, room -> null);

        // when, then — 락이 남으면 다른 서버의 변경이 대기 시간을 다 채우고 실패함
        assertThat(otherServer().mutate(roomId, room -> {
                    room.join(GUEST);
                    return "입장";
                }))
                .contains("입장");
    }

    @Test
    @DisplayName("없는 방은 고치지 않고 비어 있다고 알린다")
    default void 없는_방은_비어_있다() {
        assertThat(registry().mutate(987654L, room -> "고침")).isEmpty();
        assertThat(registry().find(987654L)).isEmpty();
    }

    @Test
    @DisplayName("마감이 지난 진행 중인 방만 턴 만료 후보다")
    default void 턴_만료_후보() {
        // given
        long roomId = 시작된_방();
        Instant deadline = Instant.now().plusSeconds(60);
        registry().mutate(roomId, room -> {
            room.restartTurnClock(deadline);
            return null;
        });

        // then
        assertThat(otherServer().findTurnExpired(deadline.minusSeconds(1))).doesNotContain(roomId);
        assertThat(otherServer().findTurnExpired(deadline)).contains(roomId);

        // when — 판이 끝나면 마감이 남아 있어도 후보가 아님
        registry().mutate(roomId, room -> room.game().apply(Stone.BLACK, new Move.Resign()));

        // then
        assertThat(otherServer().findTurnExpired(deadline)).doesNotContain(roomId);
    }

    @Test
    @DisplayName("마지막 활동이 기준보다 오래된 방만 유휴 후보다")
    default void 유휴_후보() {
        // given
        long roomId = registry().create("방", HOST).roomId();

        // then
        assertThat(otherServer().findIdle(Instant.now().minusSeconds(60))).doesNotContain(roomId);
        assertThat(otherServer().findIdle(Instant.now().plusSeconds(60))).contains(roomId);
    }

    @Test
    @DisplayName("두 서버가 같은 방을 동시에 고쳐도 한 번에 하나씩만 적용된다")
    default void 동시_변경은_하나씩() throws Exception {
        // given — 흑 차례에 흑이 동시에 20곳에 두려 함. 차례가 넘어가므로 하나만 받아들여져야 함
        long roomId = 시작된_방();
        int attempts = 20;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<MoveResult>> results = new ArrayList<>();

        // when — 절반은 이 서버, 절반은 다른 서버로
        for (int point = 0; point < attempts; point++) {
            RoomRegistry server = point % 2 == 0 ? registry() : otherServer();
            int target = point;
            results.add(pool.submit(() -> {
                start.await();
                return server.mutate(roomId, room -> room.game().apply(Stone.BLACK, new Move.Place(target)))
                        .orElseThrow();
            }));
        }
        start.countDown();
        int applied = 0;
        for (Future<MoveResult> result : results) {
            if (result.get() instanceof MoveResult.Applied) {
                applied++;
            }
        }
        pool.shutdown();

        // then — 락이 없으면 여러 서버가 같은 판을 읽고 각자 써서 여러 수가 받아들여짐
        assertThat(applied).isEqualTo(1);
        assertThat(registry().find(roomId).orElseThrow().game().onBoard(Stone.BLACK))
                .isEqualTo(1);
    }
}
