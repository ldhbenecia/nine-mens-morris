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
import com.ninemensmorris.messaging.ClusterBroadcaster;
import com.ninemensmorris.messaging.StompDestinations;
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
// roomId 는 항상 경로 변수로 받음. 페이로드에 식별자를 넣으면 조작 지점이 되고
// 목적지별 인가 검사를 일괄 적용할 수 없음
// 행위자는 Principal 에서만 옴
@Controller
@RequiredArgsConstructor
@Slf4j
public class GameStompController {

    private final GameService gameService;
    private final SimpMessagingTemplate messaging; // 개인 큐. 요청한 세션이 이 서버에 있음
    private final ClusterBroadcaster broadcaster; // 방 토픽. 모든 서버로

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

    // 2·3단계 이동. 배치와 목적지를 나눠 요청마다 쓰는 필드만 받음
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

    // 재접속 복구. 요청자에게만 현재 판을 보냄
    @MessageMapping("/rooms/{roomId}/sync")
    public void sync(@DestinationVariable long roomId, Principal principal) {
        AuthenticatedUser actor = require(principal);
        gameService
                .snapshot(roomId, actor.id())
                .ifPresent(event -> messaging.convertAndSendToUser(
                        String.valueOf(actor.id()), StompDestinations.SYNC_QUEUE, event));
    }

    // @RestControllerAdvice 는 메시징에 적용되지 않음
    // 여기서 잡지 않으면 예외가 클라이언트에 닿지 않고 서버 로그에만 남아 화면은 아무 반응이 없음
    @MessageExceptionHandler(CustomException.class)
    public void onCustomException(CustomException exception, Principal principal) {
        sendError(principal, ErrorResponse.of(exception.getErrorCode()));
    }

    // @Valid 가 걸린 @Payload 의 검증 실패
    // 웹 계층의 동명 예외가 아니라 messaging 쪽 예외가 날아옴. 타입을 헷갈리면 조용히 안 잡힘
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
        messaging.convertAndSendToUser(String.valueOf(actor.id()), StompDestinations.ERROR_QUEUE, response);
    }

    private void play(long roomId, Principal principal, Move move) {
        AuthenticatedUser actor = require(principal);
        PlayOutcome outcome = gameService.play(new RoomCommand.PlayMove(actor.id(), roomId, move));

        switch (outcome) {
            case PlayOutcome.Broadcast broadcast -> broadcast(roomId, broadcast.event());
            // 거절은 요청자에게만. 방에 뿌리면 상대에게 내 실수가 보임
            case PlayOutcome.Reject reject ->
                messaging.convertAndSendToUser(
                        String.valueOf(actor.id()), StompDestinations.ERROR_QUEUE, reject.response());
        }
    }

    private void broadcast(long roomId, RoomEvent event) {
        broadcaster.sendToRoom(roomId, event);
    }

    private AuthenticatedUser require(Principal principal) {
        AuthenticatedUser actor = AuthenticatedUser.from(principal);
        if (actor == null) {
            throw new IllegalStateException("인증되지 않은 STOMP 연결");
        }
        return actor;
    }
}
