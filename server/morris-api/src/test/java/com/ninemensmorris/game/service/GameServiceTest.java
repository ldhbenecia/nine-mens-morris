package com.ninemensmorris.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ninemensmorris.common.exception.CustomException;
import com.ninemensmorris.common.response.ErrorCode;
import com.ninemensmorris.core.board.Stone;
import com.ninemensmorris.core.game.GameStatus;
import com.ninemensmorris.core.move.Move;
import com.ninemensmorris.game.command.RoomCommand;
import com.ninemensmorris.game.domain.FirstMoveRule;
import com.ninemensmorris.game.domain.InMemoryRoomRegistry;
import com.ninemensmorris.game.domain.Room;
import com.ninemensmorris.game.domain.RoomRegistry;
import com.ninemensmorris.game.domain.SessionTracker;
import com.ninemensmorris.game.dto.response.RoomEvent;
import com.ninemensmorris.game.dto.response.RoomEventType;
import com.ninemensmorris.match.service.MatchResultService;
import com.ninemensmorris.observability.GameMetrics;
import com.ninemensmorris.support.MutableClock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// Spring 컨텍스트 없이 도는 테스트. RoomRegistry 는 실제 구현을 씀
class GameServiceTest {

    private static final long HOST = 1L;
    private static final long GUEST = 2L;
    private static final long OUTSIDER = 99L;

    // 30분짜리 실제 값을 쓰면 유휴 정리를 검증할 수 없음
    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(30);
    private static final Duration TURN_LIMIT = Duration.ofSeconds(60);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-11T00:00:00Z"));
    private RoomRegistry rooms;
    private MatchResultService matchResultService;
    private SimpleMeterRegistry meterRegistry;
    private GameMetrics metrics;
    private GameService gameService;

    @BeforeEach
    void setUp() {
        rooms = new InMemoryRoomRegistry();
        matchResultService = mock(MatchResultService.class);
        // 메트릭은 목이 아니라 실물을 씀. 등록 이름이 틀리면 여기서 바로 터짐
        meterRegistry = new SimpleMeterRegistry();
        metrics = new GameMetrics(meterRegistry, rooms, new SessionTracker());
        gameService = new GameService(rooms, matchResultService, metrics, IDLE_TIMEOUT, TURN_LIMIT, clock);
    }

    private Room 시작된_방(FirstMoveRule rule) {
        Room room = rooms.create("테스트 방", HOST);
        room.join(GUEST);
        room.changeFirstMoveRule(rule);
        gameService.start(new RoomCommand.StartGame(HOST, room.roomId()));
        return room;
    }

    private PlayOutcome 착수(long actorId, long roomId, Move move) {
        return gameService.play(new RoomCommand.PlayMove(actorId, roomId, move));
    }

    @Nested
    class 게임_시작 {

        @Test
        @DisplayName("방장만 시작할 수 있다")
        void 방장만_시작한다() {
            // given
            Room room = rooms.create("방", HOST);
            room.join(GUEST);

            // then
            assertThatThrownBy(() -> gameService.start(new RoomCommand.StartGame(GUEST, room.roomId())))
                    .isInstanceOf(CustomException.class);
            assertThat(room.isPlaying()).isFalse();
        }

        @Test
        @DisplayName("두 명이 모이지 않으면 시작할 수 없다")
        void 혼자서는_시작할_수_없다() {
            // given
            Room room = rooms.create("방", HOST);

            // then
            assertThatThrownBy(() -> gameService.start(new RoomCommand.StartGame(HOST, room.roomId())))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("이미 시작된 게임은 다시 시작할 수 없다")
        void 중복_시작을_막는다() {
            // given — 다시 시작되면 진행 중인 판이 초기화됨
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // then
            assertThatThrownBy(() -> gameService.start(new RoomCommand.StartGame(HOST, room.roomId())))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("같은 상대와 이어 두는 판은 매칭 대기 시간을 재지 않는다")
        void 이어_두는_판은_대기를_재지_않는다() {
            // given — 첫 판을 끝냄
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);
            착수(HOST, room.roomId(), new Move.Resign());

            // when — 같은 상대와 한 판 더
            gameService.start(new RoomCommand.StartGame(HOST, room.roomId()));

            // then — 방 생성부터 재면 앞 판을 둔 시간까지 대기로 잡힘
            assertThat(meterRegistry.get("morris.room.wait").timer().count()).isEqualTo(1);
        }

        @Test
        @DisplayName("없는 방은 시작할 수 없다")
        void 없는_방은_시작할_수_없다() {
            // then
            assertThatThrownBy(() -> gameService.start(new RoomCommand.StartGame(HOST, 12345L)))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("HOST_FIRST 면 방장이 흑이다")
        void 방장_선공이면_방장이_흑이다() {
            // given
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // then
            assertThat(room.blackId()).isEqualTo(HOST);
            assertThat(room.whiteId()).isEqualTo(GUEST);
            assertThat(room.game().currentTurn()).isEqualTo(Stone.BLACK);
        }

        @Test
        @DisplayName("GUEST_FIRST 면 참가자가 흑이다")
        void 참가자_선공이면_참가자가_흑이다() {
            // given — 방장이 항상 흑이라는 전제가 코드에 남아 있으면 깨짐
            Room room = 시작된_방(FirstMoveRule.GUEST_FIRST);

            // then
            assertThat(room.blackId()).isEqualTo(GUEST);
            assertThat(room.whiteId()).isEqualTo(HOST);
        }
    }

    @Nested
    class 선공_설정 {

        @Test
        @DisplayName("방장만 바꿀 수 있다")
        void 방장만_바꾼다() {
            // given
            Room room = rooms.create("방", HOST);
            room.join(GUEST);

            // then
            assertThatThrownBy(() -> gameService.changeFirstMoveRule(
                            new RoomCommand.ChangeFirstMoveRule(GUEST, room.roomId(), FirstMoveRule.GUEST_FIRST)))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("시작 전에는 몇 번이든 바꿀 수 있다")
        void 시작_전에는_바꿀_수_있다() {
            // given
            Room room = rooms.create("방", HOST);
            room.join(GUEST);

            // when
            RoomEvent event = gameService.changeFirstMoveRule(
                    new RoomCommand.ChangeFirstMoveRule(HOST, room.roomId(), FirstMoveRule.GUEST_FIRST));

            // then
            assertThat(event.type()).isEqualTo(RoomEventType.SETTINGS_CHANGED);
            assertThat(room.firstMoveRule()).isEqualTo(FirstMoveRule.GUEST_FIRST);
        }

        @Test
        @DisplayName("시작한 뒤에는 바꿀 수 없다")
        void 시작_후에는_바꿀_수_없다() {
            // given
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // then
            assertThatThrownBy(() -> gameService.changeFirstMoveRule(
                            new RoomCommand.ChangeFirstMoveRule(HOST, room.roomId(), FirstMoveRule.GUEST_FIRST)))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    class 착수 {

        @Test
        @DisplayName("정상 착수는 방 전체에 브로드캐스트된다")
        void 정상_착수는_브로드캐스트된다() {
            // given
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // when
            PlayOutcome outcome = 착수(HOST, room.roomId(), new Move.Place(0));

            // then
            assertThat(outcome).isInstanceOf(PlayOutcome.Broadcast.class);
            RoomEvent event = ((PlayOutcome.Broadcast) outcome).event();
            assertThat(event.type()).isEqualTo(RoomEventType.STATE_CHANGED);
            assertThat(event.state().board()[0]).isEqualTo("BLACK");
            assertThat(event.state().currentTurnId()).isEqualTo(GUEST);
        }

        @Test
        @DisplayName("상대 차례에 두면 거절되고 요청자에게만 알린다")
        void 남의_차례는_거절된다() {
            // given
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // when
            PlayOutcome outcome = 착수(GUEST, room.roomId(), new Move.Place(0));

            // then
            assertThat(outcome).isInstanceOf(PlayOutcome.Reject.class);
            assertThat(((PlayOutcome.Reject) outcome).response().code()).isEqualTo("NOT_YOUR_TURN");
            assertThat(room.game().board().isEmpty(0)).isTrue();
        }

        @Test
        @DisplayName("방에 속하지 않은 사람의 착수는 거절된다")
        void 외부인의_착수는_거절된다() {
            // given — STOMP 프레임을 직접 만들어 남의 방에 두는 것을 막음
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // when
            PlayOutcome outcome = 착수(OUTSIDER, room.roomId(), new Move.Place(0));

            // then
            assertThat(outcome).isInstanceOf(PlayOutcome.Reject.class);
            assertThat(room.game().board().isEmpty(0)).isTrue();
        }

        @Test
        @DisplayName("시작하지 않은 방에는 둘 수 없다")
        void 시작_전에는_둘_수_없다() {
            // given
            Room room = rooms.create("방", HOST);
            room.join(GUEST);

            // when
            PlayOutcome outcome = 착수(HOST, room.roomId(), new Move.Place(0));

            // then
            assertThat(((PlayOutcome.Reject) outcome).response().code()).isEqualTo("GAME_NOT_IN_PROGRESS");
        }

        @Test
        @DisplayName("거절 메시지는 밀이 아니라 3연속으로 표현한다")
        void 거절_문구는_사용자_용어를_쓴다() {
            // given
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);
            착수(HOST, room.roomId(), new Move.Place(0));

            // when
            PlayOutcome outcome = 착수(GUEST, room.roomId(), new Move.Place(0));

            // then
            assertThat(((PlayOutcome.Reject) outcome).response().message()).isEqualTo("이미 돌이 놓인 지점입니다.");
        }
    }

    @Nested
    class 종료 {

        @Test
        @DisplayName("기권하면 게임이 끝나고 전적이 기록된다")
        void 기권하면_전적이_기록된다() {
            // given
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // when
            PlayOutcome outcome = 착수(HOST, room.roomId(), new Move.Resign());

            // then
            RoomEvent event = ((PlayOutcome.Broadcast) outcome).event();
            assertThat(event.type()).isEqualTo(RoomEventType.FINISHED);
            assertThat(event.state().status()).isEqualTo(GameStatus.FINISHED);
            assertThat(event.state().winnerId()).isEqualTo(GUEST);
            assertThat(event.state().loserId()).isEqualTo(HOST);
            verify(matchResultService).record(eq(HOST), eq(GUEST), eq(GUEST), eq("RESIGN"), anyInt());
        }

        @Test
        @DisplayName("전적 기록은 방 락을 놓은 뒤에 한다")
        void 전적_기록은_락_밖에서_한다() {
            // given — 기록 시점에 이 스레드가 방 모니터를 쥐고 있는지 잡아 둠
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);
            AtomicBoolean heldLock = new AtomicBoolean(true);
            doAnswer(invocation -> {
                        heldLock.set(Thread.holdsLock(room));
                        return null;
                    })
                    .when(matchResultService)
                    .record(anyLong(), anyLong(), any(), anyString(), anyInt());

            // when
            착수(HOST, room.roomId(), new Move.Resign());

            // then — 락 안에서 DB 를 기다리면 그동안 이 방의 모든 수가 막힘
            verify(matchResultService).record(anyLong(), anyLong(), any(), anyString(), anyInt());
            assertThat(heldLock).isFalse();
        }

        @Test
        @DisplayName("끝난 게임에는 더 둘 수 없다")
        void 끝난_게임에는_둘_수_없다() {
            // given
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);
            착수(HOST, room.roomId(), new Move.Resign());

            // when
            PlayOutcome outcome = 착수(GUEST, room.roomId(), new Move.Place(0));

            // then
            assertThat(((PlayOutcome.Reject) outcome).response().code()).isEqualTo("GAME_NOT_IN_PROGRESS");
        }
    }

    @Nested
    class 연결_끊김 {

        @Test
        @DisplayName("게임 중 끊기면 기권 처리된다")
        void 게임_중_끊기면_기권이다() {
            // given — 승패를 남기지 않으면 질 것 같을 때 창을 닫는 게 이득이 됨
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // when
            RoomBroadcast broadcast =
                    gameService.handleDisconnect(HOST, room.roomId()).orElseThrow();

            // then
            assertThat(broadcast.roomId()).isEqualTo(room.roomId());
            assertThat(broadcast.event().type()).isEqualTo(RoomEventType.OPPONENT_DISCONNECTED);
            assertThat(room.game().status()).isEqualTo(GameStatus.FINISHED);
            verify(matchResultService).record(eq(HOST), eq(GUEST), eq(GUEST), eq("RESIGN"), anyInt());
        }

        @Test
        @DisplayName("대기 중에 방장이 끊기면 방이 사라진다")
        void 대기중_방장이_끊기면_방이_사라진다() {
            // given
            Room room = rooms.create("방", HOST);
            room.join(GUEST);

            // when
            gameService.handleDisconnect(HOST, room.roomId());

            // then
            assertThat(rooms.find(room.roomId())).isEmpty();
            verify(matchResultService, never()).record(anyLong(), anyLong(), anyLong(), anyString(), anyInt());
        }

        @Test
        @DisplayName("대기 중에 참가자가 끊기면 방은 남는다")
        void 대기중_참가자가_끊기면_방은_남는다() {
            // given
            Room room = rooms.create("방", HOST);
            room.join(GUEST);

            // when
            gameService.handleDisconnect(GUEST, room.roomId());

            // then
            assertThat(rooms.find(room.roomId())).isPresent();
            assertThat(room.isFull()).isFalse();
        }

        @Test
        @DisplayName("방에 속하지 않은 사람이 끊겨도 아무 일도 일어나지 않는다")
        void 방에_없으면_무시한다() {
            // given
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // then
            assertThat(gameService.handleDisconnect(OUTSIDER, room.roomId())).isEmpty();
        }
    }

    @Nested
    class 나가기 {

        @Test
        @DisplayName("게임 중 나가면 기권 처리되고 전적이 기록된다")
        void 게임_중_나가면_기권이다() {
            // given — 나가기가 정산을 건너뛰면 지고 있을 때 나가는 쪽이 이득이 됨
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // when
            RoomBroadcast broadcast = gameService
                    .leave(new RoomCommand.LeaveRoom(GUEST, room.roomId()))
                    .orElseThrow();

            // then
            assertThat(broadcast.event().type()).isEqualTo(RoomEventType.FINISHED);
            assertThat(room.game().status()).isEqualTo(GameStatus.FINISHED);
            verify(matchResultService).record(eq(HOST), eq(GUEST), eq(HOST), eq("RESIGN"), anyInt());
        }

        @Test
        @DisplayName("정산이 끝나면 방이 대기 상태로 돌아간다")
        void 정산_후_방이_비워진다() {
            // given — 비우지 않으면 끝난 방이 로비에 남고 나간 사람 자리로 새 게임이 시작될 수 있음
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // when
            gameService.leave(new RoomCommand.LeaveRoom(GUEST, room.roomId()));

            // then
            assertThat(room.isFull()).isFalse();
            assertThat(room.isPlaying()).isFalse();
        }

        @Test
        @DisplayName("방에 속하지 않은 사람이 나가면 알릴 것이 없다")
        void 외부인의_나가기는_무시된다() {
            // given — 무조건 브로드캐스트하면 아무나 남의 방에 PLAYER_LEFT 를 꽂을 수 있음
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // when
            var broadcast = gameService.leave(new RoomCommand.LeaveRoom(OUTSIDER, room.roomId()));

            // then
            assertThat(broadcast).isEmpty();
            assertThat(room.isPlaying()).isTrue();
            verify(matchResultService, never()).record(anyLong(), anyLong(), anyLong(), anyString(), anyInt());
        }

        @Test
        @DisplayName("없는 방에서 나가도 오류가 아니다")
        void 없는_방은_무시한다() {
            // then
            assertThat(gameService.leave(new RoomCommand.LeaveRoom(HOST, 404L))).isEmpty();
        }

        @Test
        @DisplayName("대기 중에 나가면 정산 없이 퇴장만 알린다")
        void 대기중_나가면_정산하지_않는다() {
            // given
            Room room = rooms.create("방", HOST);
            room.join(GUEST);

            // when
            RoomBroadcast broadcast = gameService
                    .leave(new RoomCommand.LeaveRoom(GUEST, room.roomId()))
                    .orElseThrow();

            // then
            assertThat(broadcast.event().type()).isEqualTo(RoomEventType.PLAYER_LEFT);
            verify(matchResultService, never()).record(anyLong(), anyLong(), anyLong(), anyString(), anyInt());
        }
    }

    @Nested
    class 유휴_방_정리 {

        private GameService 즉시_정리하는_서비스() {
            return new GameService(rooms, matchResultService, metrics, Duration.ZERO, TURN_LIMIT, clock);
        }

        @Test
        @DisplayName("진행 중이던 방은 무승부로 기록하고 지운다")
        void 진행중인_방은_무승부로_정산한다() {
            // given — 정산 없이 지우면 진행 중이던 판이 기록 없이 사라짐
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // when — 임계값 0 이면 모든 방이 정리 대상이 됨
            var broadcasts = 즉시_정리하는_서비스().purgeIdleRooms();

            // then
            assertThat(broadcasts).hasSize(1);
            assertThat(broadcasts.get(0).event().type()).isEqualTo(RoomEventType.FINISHED);
            assertThat(rooms.find(room.roomId())).isEmpty();
            verify(matchResultService).record(eq(HOST), eq(GUEST), eq(null), eq("ABANDONED"), anyInt());
        }

        @Test
        @DisplayName("시작 전 방은 조용히 지운다")
        void 대기중인_방은_정산하지_않는다() {
            // given
            Room room = rooms.create("방", HOST);

            // when
            var broadcasts = 즉시_정리하는_서비스().purgeIdleRooms();

            // then
            assertThat(broadcasts).isEmpty();
            assertThat(rooms.find(room.roomId())).isEmpty();
            verify(matchResultService, never()).record(anyLong(), anyLong(), anyLong(), anyString(), anyInt());
        }

        @Test
        @DisplayName("최근까지 쓰인 방은 건드리지 않는다")
        void 활동중인_방은_남긴다() {
            // given
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // when
            gameService.purgeIdleRooms();

            // then
            assertThat(rooms.find(room.roomId())).isPresent();
        }
    }

    @Nested
    class 재접속_복구 {

        @Test
        @DisplayName("진행 중이면 현재 판을 돌려준다")
        void 진행중이면_스냅샷을_준다() {
            // given — 새로고침한 화면이 이어서 그릴 판
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);
            착수(HOST, room.roomId(), new Move.Place(5));

            // when
            RoomEvent event = gameService.snapshot(room.roomId(), HOST).orElseThrow();

            // then
            assertThat(event.type()).isEqualTo(RoomEventType.SNAPSHOT);
            assertThat(event.state().board()[5]).isEqualTo("BLACK");
        }

        @Test
        @DisplayName("시작 전이면 스냅샷이 없다")
        void 시작_전에는_스냅샷이_없다() {
            // given
            Room room = rooms.create("방", HOST);

            // then
            assertThat(gameService.snapshot(room.roomId(), HOST)).isEmpty();
        }

        @Test
        @DisplayName("방에 속하지 않은 사람은 판을 볼 수 없다")
        void 외부인에게는_스냅샷을_주지_않는다() {
            // given — roomId 만 알면 남의 판을 끌어올 수 있으면 안 됨
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // then
            assertThat(gameService.snapshot(room.roomId(), OUTSIDER)).isEmpty();
        }

        @Test
        @DisplayName("방이 사라졌으면 조용히 넘어가지 않고 알린다")
        void 없는_방이면_알린다() {
            // when / then — 알리지 않으면 서버 재시작 뒤 재접속했을 때 판이 멀쩡해 보인 채로 남음
            assertThatThrownBy(() -> gameService.snapshot(12345L, HOST))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
        }
    }

    @Nested
    class 시간_제한 {

        @Test
        @DisplayName("제한 시간이 지나면 차례인 쪽이 지고 전적이 기록된다")
        void 시간이_지나면_진다() {
            // given — 방장이 흑이라 방장 차례
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // when
            clock.move(TURN_LIMIT.plusSeconds(1));
            List<RoomBroadcast> expired = gameService.expireTurns();

            // then
            assertThat(expired).singleElement().satisfies(broadcast -> {
                assertThat(broadcast.roomId()).isEqualTo(room.roomId());
                assertThat(broadcast.event().type()).isEqualTo(RoomEventType.FINISHED);
                assertThat(broadcast.event().state().winnerId()).isEqualTo(GUEST);
                assertThat(broadcast.event().state().endReason()).isEqualTo("TIMEOUT");
            });
            verify(matchResultService).record(eq(HOST), eq(GUEST), eq(GUEST), eq("TIMEOUT"), anyInt());
        }

        @Test
        @DisplayName("정산이 실패해도 종료 알림은 나가고 같은 틱의 다른 방도 끝난다")
        void 정산이_실패해도_알림은_나간다() {
            // given — 두 방이 함께 시간 초과. DB 가 끊겨 정산이 전부 실패함
            시작된_방(FirstMoveRule.HOST_FIRST);
            Room other = rooms.create("다른 방", 3L);
            other.join(4L);
            other.changeFirstMoveRule(FirstMoveRule.HOST_FIRST);
            gameService.start(new RoomCommand.StartGame(3L, other.roomId()));
            doThrow(new IllegalStateException("DB 끊김"))
                    .when(matchResultService)
                    .record(anyLong(), anyLong(), any(), anyString(), anyInt());

            // when
            clock.move(TURN_LIMIT.plusSeconds(1));
            List<RoomBroadcast> expired = gameService.expireTurns();

            // then — 알림이 막히면 양쪽 화면에 끝난 판이 진행 중으로 남음
            assertThat(expired).hasSize(2).allSatisfy(broadcast -> assertThat(
                            broadcast.event().type())
                    .isEqualTo(RoomEventType.FINISHED));
        }

        @Test
        @DisplayName("제한 시간 전에는 끝나지 않는다")
        void 시간_전에는_그대로다() {
            // given
            시작된_방(FirstMoveRule.HOST_FIRST);

            // when
            clock.move(TURN_LIMIT.minusSeconds(1));

            // then
            assertThat(gameService.expireTurns()).isEmpty();
        }

        @Test
        @DisplayName("수를 두면 상대의 시간이 처음부터 다시 흐른다")
        void 수를_두면_다시_흐른다() {
            // given — 방장이 50초를 쓰고 둠
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);
            clock.move(Duration.ofSeconds(50));
            착수(HOST, room.roomId(), new Move.Place(0));

            // when — 시작부터는 100초, 참가자 차례가 된 뒤로는 50초
            clock.move(Duration.ofSeconds(50));

            // then
            assertThat(gameService.expireTurns()).isEmpty();
            assertThat(room.game().currentTurn()).isEqualTo(Stone.WHITE);
        }

        @Test
        @DisplayName("무승부 제안으로는 시간을 되돌릴 수 없다")
        void 무승부_제안은_시간을_되돌리지_않는다() {
            // given — 시간 끌기용으로 제안을 반복하는 경우
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);
            clock.move(Duration.ofSeconds(50));
            착수(HOST, room.roomId(), new Move.OfferDraw());

            // when
            clock.move(Duration.ofSeconds(11));

            // then
            assertThat(gameService.expireTurns()).singleElement();
        }

        @Test
        @DisplayName("판 상태에 남은 시간이 실린다")
        void 남은_시간이_실린다() {
            // given
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // when
            clock.move(Duration.ofSeconds(15));
            RoomEvent snapshot = gameService.snapshot(room.roomId(), HOST).orElseThrow();

            // then — 절대 시각이 아니라 남은 양을 줘야 클라이언트 시계가 틀려도 맞게 보임
            assertThat(snapshot.state().turnRemainingMillis()).isEqualTo(45_000L);
        }
    }
}
