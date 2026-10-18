package com.ninemensmorris.game.domain;

import com.ninemensmorris.core.board.Stone;
import com.ninemensmorris.core.game.GameStatus;
import com.ninemensmorris.core.game.MorrisGame;
import java.time.Instant;
import java.util.Random;

// 방 하나의 상태. 대기 중이면 game 이 null 이고 시작하면 채워짐
//
// DB 가 아니라 RoomRegistry(Redis) 에 둠. 방의 수명이 수십 초라 DB 행으로 두면 고아 행이 쌓임
// 저장소에서 꺼낸 사본이므로 고쳐도 RoomRegistry.mutate 를 거쳐야 저장됨
public final class Room {

    private static final Random RANDOM = new Random();

    private final long roomId;
    private final String title;
    private final long hostId;
    private final Instant createdAt;

    // 변경은 전부 RoomRegistry.mutate 의 방 단위 락 안에서 일어나지만
    // 로비 목록·방 상세·유휴 정리는 락 없이 읽음
    // volatile 이 없으면 낡은 값이나 절반만 초기화된 MorrisGame 이 보일 수 있고
    // long 은 읽기 자체가 쪼개질 수 있음
    private volatile Long guestId;
    private volatile FirstMoveRule firstMoveRule = FirstMoveRule.RANDOM;
    private volatile MorrisGame game;

    // 흑/백이 각각 누구인지. 방장이 항상 흑이라는 전제를 두지 않기 위해 분리함
    private volatile long blackId;
    private volatile long whiteId;

    private volatile Instant lastActivityAt = Instant.now();

    // 차례인 쪽이 둬야 하는 시각. 만료 확인이 락 밖에서 목록을 훑으므로 volatile
    private volatile Instant turnDeadline;

    // 변경될 때마다 하나씩 오름. 여러 서버가 보낸 이벤트가 순서가 뒤바뀌어 도착해도
    // 클라이언트가 더 오래된 판 상태를 버릴 수 있게 함
    private volatile long revision;

    // 변경 중에 방을 없애기로 했다는 표시. 저장소가 저장 대신 삭제함
    // 변경 안에서 저장소를 직접 지우면 "읽고 고쳐 쓰는" 저장소에서는 끝에 다시 써져 방이 되살아남
    private boolean closed;

    public Room(long roomId, String title, long hostId) {
        this.roomId = roomId;
        this.title = title;
        this.hostId = hostId;
        this.createdAt = Instant.now();
    }

    private Room(RoomSnapshot snapshot) {
        this.roomId = snapshot.roomId();
        this.title = snapshot.title();
        this.hostId = snapshot.hostId();
        this.createdAt = snapshot.createdAt();
        this.guestId = snapshot.guestId();
        this.firstMoveRule = snapshot.firstMoveRule();
        this.blackId = snapshot.blackId();
        this.whiteId = snapshot.whiteId();
        this.lastActivityAt = snapshot.lastActivityAt();
        this.turnDeadline = snapshot.turnDeadline();
        this.revision = snapshot.revision();
        this.game = snapshot.game() == null ? null : MorrisGame.restore(snapshot.game());
    }

    public static Room restore(RoomSnapshot snapshot) {
        return new Room(snapshot);
    }

    public RoomSnapshot snapshot() {
        return new RoomSnapshot(
                roomId,
                title,
                hostId,
                createdAt,
                guestId,
                firstMoveRule,
                blackId,
                whiteId,
                lastActivityAt,
                turnDeadline,
                revision,
                game == null ? null : game.snapshot());
    }

    // 저장소가 변경을 적용하기 직전에 부름. 변경 하나에 한 번
    // 변경 안에서 만든 판 상태(이벤트)가 저장될 revision 을 실어야 함
    // 저장 뒤에 올리면 이벤트는 옛 번호를 달고 나가, 다음 sync 와 다음 수가 같은 번호를 나눠 가짐
    public void nextRevision() {
        this.revision++;
    }

    // 메모리 저장소가 바뀐 게 없는 변경을 되돌릴 때만. Redis 저장소는 저장하지 않는 것으로 되돌림
    void undoRevision() {
        this.revision--;
    }

    public long revision() {
        return revision;
    }

    public void close() {
        this.closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isFull() {
        return guestId != null;
    }

    // 끝난 판이 남아 있어도 진행 중은 아님
    // game != null 만 보면 종료 후에도 목록에 계속 "진행 중" 으로 뜸
    public boolean isPlaying() {
        return game != null && game.status() == GameStatus.PLAYING;
    }

    // 판이 한 번이라도 시작됐는지. 재접속 복구는 끝난 판도 돌려줘야 함
    public boolean hasGame() {
        return game != null;
    }

    public boolean contains(long userId) {
        return hostId == userId || (guestId != null && guestId == userId);
    }

    public int playerCount() {
        return isFull() ? 2 : 1;
    }

    // 새 참가자가 들어오면 지난 판 기록은 버림
    public void join(long userId) {
        this.guestId = userId;
        this.game = null;
        touch();
    }

    // 끝난 판은 남겨 둠. 남은 사람이 새로고침해도 결과를 볼 수 있어야 함
    public void leaveGuest() {
        this.guestId = null;
        touch();
    }

    public void changeFirstMoveRule(FirstMoveRule rule) {
        this.firstMoveRule = rule;
        touch();
    }

    // 선공을 확정하고 게임을 시작. 난수는 반드시 서버가 뽑음
    public MorrisGame start() {
        boolean hostIsBlack =
                switch (firstMoveRule) {
                    case HOST_FIRST -> true;
                    case GUEST_FIRST -> false;
                    case RANDOM -> RANDOM.nextBoolean();
                };
        this.blackId = hostIsBlack ? hostId : guestId;
        this.whiteId = hostIsBlack ? guestId : hostId;
        this.game = new MorrisGame(Stone.BLACK);
        touch();
        return game;
    }

    public Stone stoneOf(long userId) {
        if (userId == blackId) {
            return Stone.BLACK;
        }
        if (userId == whiteId) {
            return Stone.WHITE;
        }
        return null;
    }

    public long userIdOf(Stone stone) {
        return stone == Stone.BLACK ? blackId : whiteId;
    }

    public void touch() {
        this.lastActivityAt = Instant.now();
    }

    public long roomId() {
        return roomId;
    }

    public String title() {
        return title;
    }

    public long hostId() {
        return hostId;
    }

    public Long guestId() {
        return guestId;
    }

    public FirstMoveRule firstMoveRule() {
        return firstMoveRule;
    }

    public MorrisGame game() {
        return game;
    }

    public long blackId() {
        return blackId;
    }

    public long whiteId() {
        return whiteId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant lastActivityAt() {
        return lastActivityAt;
    }

    public void restartTurnClock(Instant deadline) {
        this.turnDeadline = deadline;
    }

    public Instant turnDeadline() {
        return turnDeadline;
    }

    public boolean isTurnExpired(Instant now) {
        return isPlaying() && turnDeadline != null && !now.isBefore(turnDeadline);
    }

    // 로비 목록에 보이는 값. 이게 바뀔 때만 로비에 다시 받으라고 알림
    public LobbyView lobbyView() {
        return new LobbyView(playerCount(), isPlaying(), firstMoveRule);
    }

    public record LobbyView(int playerCount, boolean playing, FirstMoveRule firstMoveRule) {}
}
