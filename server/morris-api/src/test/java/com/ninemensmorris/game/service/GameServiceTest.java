package com.ninemensmorris.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ninemensmorris.common.exception.CustomException;
import com.ninemensmorris.core.board.Stone;
import com.ninemensmorris.core.game.GameStatus;
import com.ninemensmorris.core.move.Move;
import com.ninemensmorris.game.command.RoomCommand;
import com.ninemensmorris.game.domain.FirstMoveRule;
import com.ninemensmorris.game.domain.Room;
import com.ninemensmorris.game.domain.RoomRegistry;
import com.ninemensmorris.game.dto.response.RoomEvent;
import com.ninemensmorris.game.dto.response.RoomEventType;
import com.ninemensmorris.match.service.MatchResultService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// Spring 컨텍스트 없이 도는 테스트. RoomRegistry 는 실제 구현을 쓴다
class GameServiceTest {

    private static final long HOST = 1L;
    private static final long GUEST = 2L;
    private static final long OUTSIDER = 99L;

    // 30분짜리 실제 값을 쓰면 유휴 정리를 검증할 수 없다
    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(30);

    private RoomRegistry rooms;
    private MatchResultService matchResultService;
    private GameService gameService;

    @BeforeEach
    void setUp() {
        rooms = new RoomRegistry();
        matchResultService = mock(MatchResultService.class);
        gameService = new GameService(rooms, matchResultService, IDLE_TIMEOUT);
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
            // given — 기존에는 인증도 소속 확인도 없어 아무나 진행 중인 판을 초기화할 수 있었다
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // then
            assertThatThrownBy(() -> gameService.start(new RoomCommand.StartGame(HOST, room.roomId())))
                    .isInstanceOf(CustomException.class);
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
            // given — 방장이 항상 흑이라는 전제를 코드에서 걷어냈다
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
            // given — STOMP 프레임을 직접 만들어 남의 방에 두는 것을 막는다
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
            // given — 기존에는 방만 지우고 승패도 점수도 남기지 않아
            //         질 것 같으면 창을 닫는 게 이득이었다
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
            // given — 끊김 경로만 정산하고 이 경로는 빠뜨려서
            //         지고 있을 때 나가기를 누르는 쪽이 이득이었다
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
            // given — 정산 후에도 방이 남아 로비에 계속 뜨고
            //         방장이 없는 상대와 새 게임을 시작할 수 있었다
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
            // given — 무조건 브로드캐스트하면 아무나 남의 방에 PLAYER_LEFT 를 꽂을 수 있다
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
            return new GameService(rooms, matchResultService, Duration.ZERO);
        }

        @Test
        @DisplayName("진행 중이던 방은 무승부로 기록하고 지운다")
        void 진행중인_방은_무승부로_정산한다() {
            // given — 예전에는 RoomRegistry 가 바로 지워서 기록이 남지 않았다
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);

            // when — 임계값 0 이면 모든 방이 정리 대상이 된다
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
            // given — SYNC_GAME 이 선언만 되어 있고 새로고침하면 판을 잃었다
            Room room = 시작된_방(FirstMoveRule.HOST_FIRST);
            착수(HOST, room.roomId(), new Move.Place(5));

            // when
            RoomEvent event = gameService.snapshot(room.roomId()).orElseThrow();

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
            assertThat(gameService.snapshot(room.roomId())).isEmpty();
        }
    }
}
