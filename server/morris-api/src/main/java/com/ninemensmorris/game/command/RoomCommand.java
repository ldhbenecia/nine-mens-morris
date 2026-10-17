package com.ninemensmorris.game.command;

import com.ninemensmorris.core.move.Move;
import com.ninemensmorris.game.domain.FirstMoveRule;

// 서비스 입력 타입
//
// actorId 는 항상 첫 인자이며 컨트롤러가 Principal 에서 꺼내 채움
// 클라이언트가 보낸 페이로드에는 사용자 식별자가 없으므로 남의 id 로 조작할 수 없음
// 페이로드의 id 를 믿으면 상대 id 로 기권을 보내 자기 점수를 올릴 수 있음
public sealed interface RoomCommand {

    long actorId();

    record CreateRoom(long actorId, String title) implements RoomCommand {}

    record JoinRoom(long actorId, long roomId) implements RoomCommand {}

    record LeaveRoom(long actorId, long roomId) implements RoomCommand {}

    record ChangeFirstMoveRule(long actorId, long roomId, FirstMoveRule rule) implements RoomCommand {}

    record StartGame(long actorId, long roomId) implements RoomCommand {}

    // 착수. Move 는 좌표만 담고 행위자는 actorId 로 따로 옴
    record PlayMove(long actorId, long roomId, Move move) implements RoomCommand {}
}
