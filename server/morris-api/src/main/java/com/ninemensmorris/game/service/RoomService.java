package com.ninemensmorris.game.service;

import com.ninemensmorris.common.exception.CustomException;
import com.ninemensmorris.common.response.ErrorCode;
import com.ninemensmorris.game.command.RoomCommand;
import com.ninemensmorris.game.domain.Room;
import com.ninemensmorris.game.domain.RoomRegistry;
import com.ninemensmorris.game.dto.response.CreateRoomResponse;
import com.ninemensmorris.game.dto.response.RoomDetailResponse;
import com.ninemensmorris.game.dto.response.RoomSummaryResponse;
import com.ninemensmorris.user.domain.User;
import com.ninemensmorris.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 로비. 방 생성 / 입장 / 목록
//
// 방 자체는 Redis 에 있고 DB 는 방장 정보를 읽을 때만 씀
// 트랜잭션이 없으면 리포지터리 호출마다 커넥션을 따로 잡음
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoomService {

    private final RoomRegistry rooms;
    private final UserRepository userRepository;

    public List<RoomSummaryResponse> findAll() {
        List<Room> found = rooms.findAll();
        if (found.isEmpty()) {
            return List.of();
        }

        // 방마다 사람을 조회하면 N+1. 한 번에 가져와 맞춤
        // 랭크전 여부를 알려면 방장뿐 아니라 참가자도 회원인지 봐야 함
        Map<Long, User> players = findPlayers(found.stream().flatMap(RoomService::playerIdsOf));

        return found.stream()
                .map(room -> RoomSummaryResponse.of(room, players.get(room.hostId()), isRated(room, players)))
                .toList();
    }

    public CreateRoomResponse create(RoomCommand.CreateRoom command) {
        Room room = rooms.create(command.title(), command.actorId());
        log.info("방 생성 roomId={} hostId={}", room.roomId(), command.actorId());
        return new CreateRoomResponse(room.roomId(), room.title());
    }

    // 입장은 방 단위 락 안에서 처리함
    // 빈자리 확인과 저장 사이에 다른 요청이 끼어들면 한 자리에 두 명이 들어옴
    public void join(RoomCommand.JoinRoom command) {
        RejectableResult result = rooms.mutate(command.roomId(), room -> {
                    if (room.contains(command.actorId())) {
                        return RejectableResult.ALREADY_JOINED;
                    }
                    if (room.isFull()) {
                        return RejectableResult.FULL;
                    }
                    room.join(command.actorId());
                    return RejectableResult.OK;
                })
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        switch (result) {
            case ALREADY_JOINED -> throw new CustomException(ErrorCode.ALREADY_JOINED);
            case FULL -> throw new CustomException(ErrorCode.ROOM_FULL);
            case OK -> log.info("방 입장 roomId={} userId={}", command.roomId(), command.actorId());
        }
    }

    // 그 방 사람만 볼 수 있음. 참가자 id·닉네임까지 나가므로 방 토픽 구독과 같은 기준
    // 남이면 403 이 아니라 404. 방 번호가 순증이라 403 이면 그 번호의 방이 있다는 것이 드러남
    public RoomDetailResponse findDetail(long roomId, long actorId) {
        Room room = rooms.find(roomId)
                .filter(found -> found.contains(actorId))
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        Map<Long, User> players = findPlayers(playerIdsOf(room));
        return RoomDetailResponse.of(
                room, nicknameOf(players, room.hostId()), nicknameOf(players, room.guestId()), isRated(room, players));
    }

    private static Stream<Long> playerIdsOf(Room room) {
        return Stream.of(room.hostId(), room.guestId()).filter(Objects::nonNull);
    }

    private Map<Long, User> findPlayers(Stream<Long> userIds) {
        return userRepository.findAllById(userIds.distinct().toList()).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));
    }

    // 두 자리가 모두 회원이어야 랭크전임
    // 비로그인 계정은 얼마든지 만들 수 있어 한 명만 껴도 레이팅이 움직여선 안 됨
    //
    // 아직 상대가 없는 방은 방장 기준으로 봄. 비로그인 사용자가 들어오는 순간 일반전으로 바뀌고
    // 그 전환이 화면에 보여야 하므로 대기 중에도 값을 내려 줌
    private boolean isRated(Room room, Map<Long, User> players) {
        return playerIdsOf(room).allMatch(userId -> {
            User player = players.get(userId);
            return player != null && !player.isVisitor();
        });
    }

    private String nicknameOf(Map<Long, User> players, Long userId) {
        if (userId == null) {
            return null;
        }
        User player = players.get(userId);
        return player == null ? "알 수 없음" : player.getNickname();
    }

    private enum RejectableResult {
        OK,
        FULL,
        ALREADY_JOINED
    }
}
