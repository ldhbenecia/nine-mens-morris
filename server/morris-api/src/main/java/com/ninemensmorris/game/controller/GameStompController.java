package com.ninemensmorris.game.controller;

import com.ninemensmorris.common.exception.CustomException;
import com.ninemensmorris.common.exception.ErrorResponse;
import com.ninemensmorris.common.response.ErrorCode;
import com.ninemensmorris.core.move.Move;
import com.ninemensmorris.game.command.RoomCommand;
import com.ninemensmorris.game.dto.request.FirstMoveRuleRequest;
import com.ninemensmorris.game.dto.request.MoveRequest;
import com.ninemensmorris.game.dto.request.PlaceRequest;
import com.ninemensmorris.game.dto.request.RemoveRequest;
import com.ninemensmorris.game.dto.response.RoomEvent;
import com.ninemensmorris.game.service.GameService;
import com.ninemensmorris.game.service.PlayOutcome;
import com.ninemensmorris.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

// 게임 진행 STOMP 목적지
//
// roomId 는 항상 경로 변수로 받는다. 페이로드에 식별자를 넣으면 조작 지점이 되고
// 목적지별 인가 검사를 일괄 적용할 수 없다
// 행위자는 Principal 에서만 온다
@Controller
@RequiredArgsConstructor
@Slf4j
public class GameStompController {

    private static final String ROOM_TOPIC = "/topic/rooms/";
    private static final String ERROR_QUEUE = "/queue/errors";

    private final GameService gameService;
    private final SimpMessagingTemplate messaging;

    @MessageMapping("/rooms/{roomId}/settings")
    public void changeSettings(
            @DestinationVariable long roomId, @Valid @Payload FirstMoveRuleRequest request, Principal principal) {
        AuthenticatedUser actor = require(principal);
        RoomEvent event = gameService.changeFirstMoveRule(
                new RoomCommand.ChangeFirstMoveRule(actor.id(), roomId, request.firstMoveRule()));
        broadcast(roomId, event);
    }

    @MessageMapping("/rooms/{roomId}/start")
    public void start(@DestinationVariable long roomId, Principal principal) {
        AuthenticatedUser actor = require(principal);
        broadcast(roomId, gameService.start(new RoomCommand.StartGame(actor.id(), roomId)));
    }

    // 1단계 배치
    @MessageMapping("/rooms/{roomId}/place")
    public void place(@DestinationVariable long roomId, @Valid @Payload PlaceRequest request, Principal principal) {
        play(roomId, principal, new Move.Place(request.to()));
    }

    // 2·3단계 이동. 기존에는 배치와 같은 목적지를 써서 쓰지 않는 필드가 섞여 있었다
    @MessageMapping("/rooms/{roomId}/move")
    public void move(@DestinationVariable long roomId, @Valid @Payload MoveRequest request, Principal principal) {
        play(roomId, principal, new Move.Slide(request.from(), request.to()));
    }

    @MessageMapping("/rooms/{roomId}/remove")
    public void remove(@DestinationVariable long roomId, @Valid @Payload RemoveRequest request, Principal principal) {
        play(roomId, principal, new Move.Remove(request.at()));
    }

    @MessageMapping("/rooms/{roomId}/resign")
    public void resign(@DestinationVariable long roomId, Principal principal) {
        play(roomId, principal, new Move.Resign());
    }

    @MessageMapping("/rooms/{roomId}/draw-offer")
    public void offerDraw(@DestinationVariable long roomId, Principal principal) {
        play(roomId, principal, new Move.OfferDraw());
    }

    @MessageMapping("/rooms/{roomId}/draw-accept")
    public void acceptDraw(@DestinationVariable long roomId, Principal principal) {
        play(roomId, principal, new Move.RespondDraw(true));
    }

    @MessageMapping("/rooms/{roomId}/draw-decline")
    public void declineDraw(@DestinationVariable long roomId, Principal principal) {
        play(roomId, principal, new Move.RespondDraw(false));
    }

    // 재접속 복구. 요청자에게만 현재 판을 보낸다
    @MessageMapping("/rooms/{roomId}/sync")
    public void sync(@DestinationVariable long roomId, Principal principal) {
        AuthenticatedUser actor = require(principal);
        gameService
                .snapshot(roomId)
                .ifPresent(event -> messaging.convertAndSendToUser(String.valueOf(actor.id()), "/queue/sync", event));
    }

    // @RestControllerAdvice 는 메시징에 적용되지 않아서 여기서 던진 예외는
    // 클라이언트에 도달하지 않고 Spring 이 ERROR 스택트레이스만 찍었다
    // 방장이 아닌 사람이 시작을 눌러도 화면에 아무 반응이 없던 이유
    @MessageExceptionHandler(CustomException.class)
    public void onCustomException(CustomException exception, Principal principal) {
        sendError(principal, ErrorResponse.of(exception.getErrorCode()));
    }

    // @Valid 가 걸린 @Payload 의 검증 실패
    // 웹 계층의 동명 예외가 아니라 messaging 쪽 예외가 날아온다. 타입을 헷갈리면 조용히 안 잡힌다
    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    public void onInvalidPayload(MethodArgumentNotValidException exception, Principal principal) {
        log.debug("STOMP 페이로드 검증 실패: {}", exception.getMessage());
        sendError(principal, ErrorResponse.of(ErrorCode.INVALID_REQUEST));
    }

    private void sendError(Principal principal, ErrorResponse response) {
        AuthenticatedUser actor = AuthenticatedUser.from(principal);
        if (actor == null) {
            return;
        }
        messaging.convertAndSendToUser(String.valueOf(actor.id()), ERROR_QUEUE, response);
    }

    private void play(long roomId, Principal principal, Move move) {
        AuthenticatedUser actor = require(principal);
        PlayOutcome outcome = gameService.play(new RoomCommand.PlayMove(actor.id(), roomId, move));

        switch (outcome) {
            case PlayOutcome.Broadcast broadcast -> broadcast(roomId, broadcast.event());
            // 거절은 요청자에게만. 방에 뿌리면 상대에게 내 실수가 보인다
            case PlayOutcome.Reject reject ->
                messaging.convertAndSendToUser(String.valueOf(actor.id()), ERROR_QUEUE, reject.response());
        }
    }

    private void broadcast(long roomId, RoomEvent event) {
        messaging.convertAndSend(ROOM_TOPIC + roomId, event);
    }

    private AuthenticatedUser require(Principal principal) {
        AuthenticatedUser actor = AuthenticatedUser.from(principal);
        if (actor == null) {
            throw new IllegalStateException("인증되지 않은 STOMP 연결");
        }
        return actor;
    }
}
