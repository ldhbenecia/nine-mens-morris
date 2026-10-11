package com.ninemensmorris.game.service;

import com.ninemensmorris.common.exception.CustomException;
import com.ninemensmorris.common.response.ErrorCode;
import com.ninemensmorris.core.board.Stone;
import com.ninemensmorris.core.game.MorrisGame;
import com.ninemensmorris.core.move.Move;
import com.ninemensmorris.core.move.MoveResult;
import com.ninemensmorris.core.move.RejectReason;
import com.ninemensmorris.game.command.RoomCommand;
import com.ninemensmorris.game.domain.ExitCause;
import com.ninemensmorris.game.domain.Room;
import com.ninemensmorris.game.domain.RoomRegistry;
import com.ninemensmorris.game.dto.response.GameStateResponse;
import com.ninemensmorris.game.dto.response.MoveRejectedResponse;
import com.ninemensmorris.game.dto.response.RoomEvent;
import com.ninemensmorris.game.dto.response.RoomEventType;
import com.ninemensmorris.match.service.MatchResultService;
import com.ninemensmorris.observability.GameMetrics;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// 게임 진행. 규칙 판단은 전부 MorrisGame 이 하고 여기서는 방과 사용자에 연결만 함
@Service
@Slf4j
public class GameService {

    private final RoomRegistry rooms;
    private final MatchResultService matchResultService;
    private final GameMetrics metrics;
    private final Duration idleTimeout;
    private final Duration turnTimeLimit;
    private final Clock clock; // 한 수 제한 시간 계산에만 씀

    @Autowired
    public GameService(
            RoomRegistry rooms,
            MatchResultService matchResultService,
            GameMetrics metrics,
            @Value("${game.room.idle-timeout:30m}") Duration idleTimeout,
            @Value("${game.turn-time-limit:60s}") Duration turnTimeLimit) {
        this(rooms, matchResultService, metrics, idleTimeout, turnTimeLimit, Clock.systemUTC());
    }

    // 테스트에서 시간을 옮겨 제한 시간 경계를 확인하는 용도
    GameService(
            RoomRegistry rooms,
            MatchResultService matchResultService,
            GameMetrics metrics,
            Duration idleTimeout,
            Duration turnTimeLimit,
            Clock clock) {
        this.rooms = rooms;
        this.matchResultService = matchResultService;
        this.metrics = metrics;
        this.idleTimeout = idleTimeout;
        this.turnTimeLimit = turnTimeLimit;
        this.clock = clock;
    }

    // 선공 방식 변경. 방장만, 시작 전에만
    // 방을 다시 만들지 않고 바꿀 수 있어야 해서 생성 시점이 아니라 여기서 정한다
    public RoomEvent changeFirstMoveRule(RoomCommand.ChangeFirstMoveRule command) {
        return inRoom(command.roomId(), room -> {
            requireHost(room, command.actorId());
            if (room.isPlaying()) {
                throw new CustomException(ErrorCode.GAME_ALREADY_STARTED);
            }
            room.changeFirstMoveRule(command.rule());
            return RoomEvent.by(RoomEventType.SETTINGS_CHANGED, command.actorId());
        });
    }

    // 게임 시작. 방장만 호출할 수 있고 두 명이 모여 있어야 함
    // 기존에는 인증도 소속 확인도 없어 아무나 진행 중인 판을 초기화할 수 있었다
    public RoomEvent start(RoomCommand.StartGame command) {
        return inRoom(command.roomId(), room -> {
            requireHost(room, command.actorId());
            if (!room.isFull()) {
                throw new CustomException(ErrorCode.NOT_ENOUGH_PLAYERS);
            }
            if (room.isPlaying()) {
                throw new CustomException(ErrorCode.GAME_ALREADY_STARTED);
            }

            MorrisGame game = room.start();
            restartTurnClock(room);
            // 방을 만들고 상대를 기다린 시간. 이 서비스에서 "매칭 대기" 에 해당하는 유일한 구간
            metrics.recordRoomWait(Duration.between(room.createdAt(), Instant.now()));
            log.info(
                    "게임 시작 roomId={} black={} white={} 선공규칙={}",
                    room.roomId(),
                    room.blackId(),
                    room.whiteId(),
                    room.firstMoveRule());
            return RoomEvent.of(RoomEventType.STARTED, stateOf(room, game));
        });
    }

    // 착수. 방 단위 락 안에서 규칙 엔진에 위임한다
    public PlayOutcome play(RoomCommand.PlayMove command) {
        return afterUnlock(inRoom(command.roomId(), room -> applyMove(room, command.actorId(), command.move())));
    }

    private LockedResult<PlayOutcome> applyMove(Room room, long actorId, Move move) {
        if (!room.isPlaying()) {
            return LockedResult.of(reject(RejectReason.GAME_NOT_IN_PROGRESS));
        }
        Stone actor = room.stoneOf(actorId);
        if (actor == null) {
            // 방에 속하지 않은 사람이 보낸 프레임
            return LockedResult.of(reject(RejectReason.NOT_YOUR_TURN));
        }

        MorrisGame game = room.game();
        MoveResult result = game.apply(actor, move);

        return switch (result) {
            case MoveResult.Rejected rejected -> {
                logRejection(room.roomId(), actorId, rejected.reason(), move);
                yield LockedResult.of(reject(rejected.reason()));
            }
            case MoveResult.Finished finished ->
                LockedResult.settled(
                        broadcast(RoomEventType.FINISHED, room, game), Settlement.of(room, game, finished.outcome()));
            // 무승부 제안과 거절은 판이 안 바뀌므로 누가 했는지를 실어 보내야 함
            case MoveResult.DrawOffered ignored ->
                LockedResult.of(new PlayOutcome.Broadcast(RoomEvent.by(RoomEventType.DRAW_OFFERED, actorId)));
            case MoveResult.DrawDeclined ignored ->
                LockedResult.of(new PlayOutcome.Broadcast(RoomEvent.by(RoomEventType.DRAW_DECLINED, actorId)));
            // 판이 바뀐 수만 시계를 새로 돌림. 무승부 제안은 위에서 따로 끝나 시간 끌기에 못 씀
            // 3연속 뒤 제거도 한 번의 행동이라 새 시간을 줌
            case MoveResult.MillFormed ignored -> {
                restartTurnClock(room);
                yield LockedResult.of(broadcast(RoomEventType.STATE_CHANGED, room, game));
            }
            case MoveResult.Applied ignored -> {
                restartTurnClock(room);
                yield LockedResult.of(broadcast(RoomEventType.STATE_CHANGED, room, game));
            }
        };
    }

    // 상대 차례에 누르거나 이미 찬 자리를 누르는 건 평범한 오터치라 DEBUG
    // 전부 WARN 으로 남기면 운영자가 봐야 할 것이 묻힌다
    // 판 밖 좌표는 DTO 검증을 통과할 수 없으므로 클라이언트를 우회한 흔적임
    private void logRejection(long roomId, long actorId, RejectReason reason, Move move) {
        if (reason == RejectReason.OUT_OF_BOARD) {
            log.warn("판 밖 좌표 요청 roomId={} userId={} 수={}", roomId, actorId, move);
            return;
        }
        log.debug("수 거절 roomId={} userId={} 사유={} 수={}", roomId, actorId, reason, move);
    }

    // 소켓이 끊긴 사용자를 방에서 내보냄
    // 어느 방이었는지는 호출자가 알려줘야 함
    // 사용자로 방을 찾으면 여러 방에 속한 경우 임의의 방이 뽑혀 진행 중인 판이 방치됨
    public Optional<RoomBroadcast> handleDisconnect(long userId, long roomId) {
        return exit(roomId, userId, ExitCause.DISCONNECT);
    }

    // 스스로 나가기를 누른 경우. 끊김과 같은 경로를 타야 함
    // 예전에는 여기만 정산을 건너뛰어서 지고 있을 때 나가는 쪽이 이득이었다
    public Optional<RoomBroadcast> leave(RoomCommand.LeaveRoom command) {
        return exit(command.roomId(), command.actorId(), ExitCause.LEAVE);
    }

    // 게임이 끝나는 경로는 착수·끊김·나가기·유휴정리 네 개이고 전부 이 루틴을 거쳐야 함
    // 경로마다 따로 구현했더니 세 개가 정산을 빠뜨리고 있었다
    // 방에 속하지 않은 사람의 요청이면 아무것도 알리지 않는다
    private Optional<RoomBroadcast> exit(long roomId, long userId, ExitCause cause) {
        return rooms.mutate(roomId, room -> {
                    if (!room.contains(userId)) {
                        return null;
                    }
                    if (!room.isPlaying()) {
                        removeFromRoom(room, userId);
                        return LockedResult.of(
                                new RoomBroadcast(roomId, RoomEvent.by(RoomEventType.PLAYER_LEFT, userId)));
                    }

                    MorrisGame game = room.game();
                    Settlement settlement = null;
                    if (game.apply(room.stoneOf(userId), new Move.Resign()) instanceof MoveResult.Finished finished) {
                        settlement = Settlement.of(room, game, finished.outcome());
                        log.warn("게임 중 이탈로 기권 처리 roomId={} userId={} 사유={}", roomId, userId, cause);
                    }

                    RoomEventType type = cause == ExitCause.DISCONNECT
                            ? RoomEventType.OPPONENT_DISCONNECTED
                            : RoomEventType.FINISHED;
                    RoomEvent event = RoomEvent.of(type, stateOf(room, game));
                    removeFromRoom(room, userId);
                    return LockedResult.settled(new RoomBroadcast(roomId, event), settlement);
                })
                .map(this::afterUnlock);
    }

    // 방장이 나가면 방이 사라지고, 참가자가 나가면 방은 대기 상태로 돌아감
    // 정산 후에도 방을 남겨 두면 로비에 계속 뜨고, 방장이 없는 상대와 새 게임을 시작할 수 있다
    private void removeFromRoom(Room room, long userId) {
        if (room.hostId() == userId) {
            room.close();
            log.info("방 삭제 roomId={} 방장 퇴장", room.roomId());
        } else {
            room.leaveGuest();
            log.info("방 퇴장 roomId={} userId={}", room.roomId(), userId);
        }
    }

    // 제한 시간을 넘긴 판을 끝냄. 차례인 쪽의 패배로 기록하고 알림
    public List<RoomBroadcast> expireTurns() {
        Instant now = clock.instant();
        List<RoomBroadcast> broadcasts = new ArrayList<>();
        for (long roomId : rooms.findTurnExpired(now)) {
            rooms.mutate(roomId, room -> expireIfDue(room, now))
                    .map(this::afterUnlock)
                    .ifPresent(broadcasts::add);
        }
        return broadcasts;
    }

    // 목록을 뜬 뒤 락을 잡기 전에 수가 들어왔을 수 있어 락 안에서 다시 확인함
    private LockedResult<RoomBroadcast> expireIfDue(Room room, Instant now) {
        if (!room.isTurnExpired(now)) {
            return null;
        }
        MorrisGame game = room.game();
        if (!(game.timeout() instanceof MoveResult.Finished finished)) {
            return null;
        }
        log.info(
                "시간 초과 roomId={} 진 쪽={}",
                room.roomId(),
                room.userIdOf(finished.outcome().winner().opponent()));
        RoomEvent event = RoomEvent.of(RoomEventType.FINISHED, stateOf(room, game));
        return LockedResult.settled(
                new RoomBroadcast(room.roomId(), event), Settlement.of(room, game, finished.outcome()));
    }

    private void restartTurnClock(Room room) {
        room.restartTurnClock(clock.instant().plus(turnTimeLimit));
    }

    private GameStateResponse stateOf(Room room, MorrisGame game) {
        return GameStateResponse.of(room, game, clock.instant());
    }

    // 방치된 방 정리. 진행 중이던 판은 무승부로 기록하고 알림
    // 예전에는 RoomRegistry 가 직접 지워서 진행 중인 판이 기록 없이 사라졌다
    public List<RoomBroadcast> purgeIdleRooms() {
        Instant threshold = Instant.now().minus(idleTimeout);
        List<Long> purged = new ArrayList<>();
        List<RoomBroadcast> broadcasts = new ArrayList<>();
        for (long roomId : rooms.findIdle(threshold)) {
            rooms.mutate(roomId, room -> purgeIfIdle(room, threshold)).ifPresent(locked -> {
                purged.add(roomId);
                RoomBroadcast broadcast = afterUnlock(locked);
                if (broadcast != null) {
                    broadcasts.add(broadcast);
                }
            });
        }

        if (!purged.isEmpty()) {
            log.info("유휴 방 정리 roomId={} 그중 진행 중이던 방 {}개", purged, broadcasts.size());
        }
        return broadcasts;
    }

    // 목록을 뜬 뒤 락을 잡기 전에 누가 들어왔을 수 있어 락 안에서 유휴 여부를 다시 확인함
    // 삭제도 락 안에서 해야 그 사이 입장한 사람의 방이 사라지지 않음
    private LockedResult<RoomBroadcast> purgeIfIdle(Room room, Instant threshold) {
        if (!room.lastActivityAt().isBefore(threshold)) {
            return null;
        }
        room.close();
        if (!room.isPlaying()) {
            return LockedResult.of(null);
        }

        MorrisGame game = room.game();
        Settlement settlement = null;
        if (game.abandon() instanceof MoveResult.Finished finished) {
            settlement = Settlement.of(room, game, finished.outcome());
            log.warn("장시간 방치로 무승부 처리 roomId={}", room.roomId());
        }
        RoomEvent event = RoomEvent.of(RoomEventType.FINISHED, stateOf(room, game));
        return LockedResult.settled(new RoomBroadcast(room.roomId(), event), settlement);
    }

    // 재접속 복구. 기존에는 SYNC_GAME 이 선언만 되어 있고 새로고침하면 판을 잃었다
    // 끝난 판도 돌려줌. 결과 화면에서 새로고침해도 승패는 남아 있어야 함
    //
    // 반드시 방 단위 락 안에서 읽어야 함
    // 락 밖에서 읽으면 상대가 돌을 옮기는 중간에 보드가 복제돼 돌이 출발지에도
    // 도착지에도 없는 판이 나가고, 승부가 난 순간에는 status 만 FINISHED 이고
    // outcome 은 아직 안 보여서 결정된 판이 무승부로 그려진다
    // 방에 속한 사람만. 예전에는 roomId 만 보고 돌려줘서 아무나 남의 판을 끌어올 수 있었다
    // 방 자체가 없으면 알림. 재시작·유휴 정리 뒤 다시 붙은 화면이 판이 살아 있는 것처럼 남지 않게
    public Optional<RoomEvent> snapshot(long roomId, long actorId) {
        if (rooms.find(roomId).isEmpty()) {
            throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
        }
        return rooms.mutate(roomId, room -> {
            if (!room.contains(actorId) || !room.hasGame()) {
                return null;
            }
            return RoomEvent.of(RoomEventType.SNAPSHOT, stateOf(room, room.game()));
        });
    }

    // 반드시 방 락 밖에서 호출. 안에서 DB 를 기다리면 그동안 그 방의 모든 프레임이 막힘
    // 끝나는 경로(착수·이탈·끊김·유휴 정리)가 전부 여기를 지남
    private <R> R afterUnlock(LockedResult<R> locked) {
        Settlement settlement = locked.settlement();
        if (settlement != null) {
            matchResultService.record(
                    settlement.blackId(),
                    settlement.whiteId(),
                    settlement.winnerId(),
                    settlement.outcome().reason().name(),
                    settlement.moves());
            metrics.gameFinished(settlement.outcome().reason());
        }
        return locked.value();
    }

    private PlayOutcome broadcast(RoomEventType type, Room room, MorrisGame game) {
        return new PlayOutcome.Broadcast(RoomEvent.of(type, stateOf(room, game)));
    }

    // 거절이 나가는 유일한 지점. 여기서 세지 않으면 경로마다 빠뜨린다
    private PlayOutcome reject(RejectReason reason) {
        metrics.moveRejected(reason);
        return new PlayOutcome.Reject(MoveRejectedResponse.of(reason));
    }

    private <R> R inRoom(long roomId, java.util.function.Function<Room, R> action) {
        return rooms.mutate(roomId, action).orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
    }

    private void requireHost(Room room, long actorId) {
        if (room.hostId() != actorId) {
            throw new CustomException(ErrorCode.NOT_ROOM_HOST);
        }
    }
}
