import { useCallback, useEffect, useEffectEvent, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Client } from '@stomp/stompjs';
import { AlertModal } from '~/components';
import { authHeaders } from '~/lib/auth';
import { resultOf } from '~/lib/gameResult';
import { QUERY } from '~/lib/queries';
import { useCountdown, useGameState, useLeaveRoom } from '~/hooks';
import { FirstMoveRule, MoveRejected, RoomEvent } from '~/lib/types';
import { Board, StoneMotion } from './Board';
import { WaitingRoom } from './WaitingRoom';
import { Status } from './Status';
import { ResignModal } from './ResignModal';
import { GameResultModal } from './GameResultModal';
import { HelpModal } from './HelpModal';
import { RequestDrawModal } from './RequestDrawModal';
import { ResponseDrawModal } from './ResponseDrawModal';
import { Message } from './Message';
import {
  joinSound,
  lossSound,
  winSound,
  blackStoneSound,
  stoneDroppingSound,
  whiteStoneSound,
  startSound,
  explosionSound,
  errorSound,
  notificationSound,
  preloadGameSounds,
} from '~/lib/sounds';
import { DrawRejectedModal } from './DrawRejectedModal';
import { SocketErrorModal } from './SocketErrorModal';

// 토큰은 CONNECT 프레임 헤더로 보낸다
// 핸드셰이크는 브라우저가 보내는 HTTP 요청이라 헤더를 붙일 수 없고, 쿠키는 cross-site 라 못 쓴다
// beforeConnect 에서 채워야 재연결 때도 최신 토큰이 실린다
const client = new Client({
  brokerURL: import.meta.env.VITE_SOCKET_URL,
  reconnectDelay: 5000,
  heartbeatIncoming: 4000,
  heartbeatOutgoing: 4000,
  beforeConnect: () => {
    client.connectHeaders = authHeaders();
  },
});

export function GamePage() {
  const { roomId } = useParams();
  const [showResignModal, setShowResignModal] = useState(false);
  const [showRequestDrawModal, setShowRequestDrawModal] = useState(false);
  const [showResponseDrawModal, setShowResponseDrawModal] = useState(false);
  const [showDrawRejectedModal, setShowDrawRejectedModal] = useState(false);
  const [showHelpModal, setShowHelpModal] = useState(false);
  const [endNotice, setEndNotice] = useState('');
  const [disconnected, setDisconnected] = useState(false);
  const [lastPoint, setLastPoint] = useState<number | null>(null);
  const [motion, setMotion] = useState<StoneMotion | null>(null);
  const [roomLost, setRoomLost] = useState(false);
  const [turnDeadline, setTurnDeadline] = useState<number | null>(null);
  const secondsLeft = useCountdown(turnDeadline);
  const { data: currentUser, isFetching: fetchingUser } = useQuery(
    QUERY.CURRENT_USER
  );
  const queryClient = useQueryClient();
  const [mmrBefore, setMmrBefore] = useState<number | null>(null);
  const { mutate: leaveRoom } = useLeaveRoom();
  const {
    gameState,
    error,
    setGameState,
    showError,
    isPlayerTurn,
    myStone,
    enemyStone,
    enemyId,
    getPlayerInHand,
    getEnemyInHand,
    getPlayerOnBoard,
    getEnemyOnBoard,
    getPlayerPhase,
    addStone,
    moveStone,
    removeStone,
    resign,
    offerDraw,
    acceptDraw,
    declineDraw,
    startGame,
  } = useGameState();

  // 게임 시작 전에는 GameState 가 비어 있어 방장이 누구인지 알 수 없음
  const {
    data: room,
    isError: roomGone,
    refetch: refetchRoom,
  } = useQuery({
    ...QUERY.ROOM(Number(roomId)),
    enabled: !!roomId,
  });
  const isHost = !!currentUser && room?.hostId === currentUser.userId;

  // 'WAITING' 은 서버가 보내지 않는 값이다. 초기 상태값일 뿐이라
  // 이것만 보고 분기하면 진행 중인 판을 새로고침했을 때 대기실이 뜬다
  // 서버가 알려주는 hasGame 을 같이 본다
  const inGame = gameState.status !== 'WAITING' || room?.hasGame === true;

  const { data: enemy } = useQuery({
    ...QUERY.USER_PROFILE(enemyId()),
    enabled: gameState.status !== 'WAITING' && enemyId() > 0,
  });

  const onLeaveRoom = () => {
    if (roomId) leaveRoom(Number(roomId));
  };

  const onChangeFirstMoveRule = (rule: FirstMoveRule) => {
    if (!roomId) return;
    client.publish({
      destination: `/app/rooms/${roomId}/settings`,
      body: JSON.stringify({ firstMoveRule: rule }),
    });
  };

  const onResign = () => {
    if (roomId) resign(client, Number(roomId));
    setShowResignModal(false);
  };

  const onOfferDraw = () => {
    if (roomId) offerDraw(client, Number(roomId));
    setShowRequestDrawModal(false);
  };

  const onAcceptDraw = () => {
    if (roomId) acceptDraw(client, Number(roomId));
    setShowResponseDrawModal(false);
  };

  const onDeclineDraw = () => {
    if (roomId) declineDraw(client, Number(roomId));
    setShowResponseDrawModal(false);
  };

  // 화면에서 먼저 알아챈 거절도 서버 거절과 같은 문구 자리·소리로
  const onReject = (message: string) => {
    showError(message);
    errorSound.play();
  };

  const onStart = () => {
    if (roomId) startGame(client, Number(roomId));
  };

  // 착수 효과음. 예전에는 클라이언트가 /topic 으로 직접 발행해 서로에게 알렸는데
  // 서버가 그 경로를 막았고, 애초에 착수 결과가 브로드캐스트되므로 필요 없다
  const playMoveSound = useCallback((before: string[], after: string[]) => {
    const added = addedPoint(before, after);
    const removed = removedPoint(before, after);

    if (removed !== -1 && added === -1) {
      stoneDroppingSound.play();
      return;
    }
    if (added === -1) return;

    const sound = after[added] === 'BLACK' ? blackStoneSound : whiteStoneSound;
    sound.play();
  }, []);

  // 소켓 구독 콜백은 연결 때 한 번 등록되지만 안에서 쓰는 currentUser 는 나중에 도착함
  // 일반 함수를 넘기면 연결 시점 값을 계속 붙잡아, 무승부를 신청한 사람이 자기 신청 창을 받음
  // useEffectEvent 로 감싸면 등록된 콜백이 호출될 때마다 최신 값을 봄
  const onRoomEvent = useEffectEvent((event: RoomEvent) => {
    // 남은 시간은 받은 순간부터 셈. 서버 시각과 내 시계가 어긋나도 마감이 맞음
    if (event.state) {
      const remaining = event.state.turnRemainingMillis;
      setTurnDeadline(remaining === null ? null : Date.now() + remaining);
    }
    switch (event.type) {
      case 'STARTED':
        if (event.state) setGameState(event.state);
        setLastPoint(null);
        setMotion(null);
        startSound.play();
        break;
      case 'SNAPSHOT':
        if (event.state) setGameState(event.state);
        setLastPoint(null);
        setMotion(null);
        break;
      case 'STATE_CHANGED': {
        if (!event.state) break;
        const before = gameState.board;
        const added = addedPoint(before, event.state.board);
        const removed = removedPoint(before, event.state.board);
        // 제거만 일어난 수는 새로 생긴 돌이 없어 마지막 수 표시를 그대로 둠
        if (added !== -1) setLastPoint(added);
        setMotion(
          added !== -1 && removed !== -1
            ? { kind: 'MOVE', from: removed, to: added }
            : removed !== -1
              ? { kind: 'REMOVE', point: removed, stone: before[removed] }
              : null
        );
        setGameState((previous) => {
          playMoveSound(previous.board, event.state!.board);
          return event.state!;
        });
        break;
      }
      case 'FINISHED':
        if (event.state) setGameState(event.state);
        break;
      case 'DRAW_OFFERED':
        if (event.actorId !== currentUser?.userId) {
          setShowResponseDrawModal(true);
          notificationSound.play();
        }
        break;
      case 'DRAW_DECLINED':
        if (event.actorId !== currentUser?.userId) {
          setShowDrawRejectedModal(true);
        }
        break;
      case 'OPPONENT_DISCONNECTED':
        if (event.state) setGameState(event.state);
        setEndNotice('상대방의 연결이 끊겼습니다.');
        break;
      // 대기실 상태가 바뀌었으니 방 정보를 다시 받음
      case 'PLAYER_JOINED':
      case 'PLAYER_LEFT':
      case 'SETTINGS_CHANGED':
        refetchRoom();
        break;
      case 'LOBBY_CHANGED':
        break;
    }
  });

  const onError = useEffectEvent((message: string) => showError(message));

  useEffect(() => preloadGameSounds(), []);

  useEffect(() => {
    if (!roomId) return;

    client.onConnect = () => {
      setDisconnected(false);

      client.subscribe(`/topic/rooms/${roomId}`, (message) => {
        onRoomEvent(JSON.parse(message.body) as RoomEvent);
      });

      // 규칙 위반 거절과 권한 오류는 방이 아니라 나에게만 옴
      client.subscribe('/user/queue/errors', (message) => {
        const rejected = JSON.parse(message.body) as MoveRejected;
        // 판은 서버 메모리에만 있어 재시작·정리 뒤 다시 붙으면 방이 없음. 이어 둘 수 없으니 창으로 알리고 내보냄
        if (rejected.code === 'ROOM_NOT_FOUND') {
          setRoomLost(true);
          return;
        }
        onError(rejected.message);
        // 폭발음은 3연속 전용. 거절에까지 쓰면 둘을 소리로 구분할 수 없음
        errorSound.play();
      });

      // 새로고침 후 판 복구
      client.subscribe('/user/queue/sync', (message) => {
        onRoomEvent(JSON.parse(message.body) as RoomEvent);
      });

      // 구독 직후 진행 중인 판이 있는지 물어본다. 새로고침해도 판을 잃지 않는다
      client.publish({ destination: `/app/rooms/${roomId}/sync` });
      joinSound.play();
    };

    // 끊긴 상태에서 publish 하면 stompjs 가 throw 하므로 화면에 알려야 한다
    client.onWebSocketClose = () => setDisconnected(true);
    client.onStompError = (frame) =>
      onError(frame.headers.message ?? '서버와 통신에 실패했습니다.');

    client.activate();

    return () => {
      // 의도적으로 끊는 것이므로 끊김 알림이 뜨지 않게 먼저 떼어낸다
      client.onWebSocketClose = () => {};
      client.deactivate();
    };
  }, [roomId]);

  const myTurn = isPlayerTurn();
  useEffect(() => {
    if (gameState.awaitingRemoval && myTurn) {
      explosionSound.play();
    }
  }, [gameState.awaitingRemoval, myTurn]);

  // 결과 화면은 상태에서 바로 계산. FINISHED 이벤트를 놓치고 SNAPSHOT 으로 복구해도 뜸
  const { status, winnerId, loserId } = gameState;
  const finished = status === 'FINISHED';
  // 객체 대신 id 에 걸어야 함. 끝난 뒤 내 정보를 다시 받으면 객체가 바뀌어 소리가 두 번 남
  const myId = currentUser?.userId;
  useEffect(() => {
    if (status !== 'FINISHED' || myId === undefined) return;
    if (winnerId === myId) winSound.play();
    if (loserId === myId) lossSound.play();
  }, [status, winnerId, loserId, myId]);

  // 서버는 결과를 저장한 뒤에 종료를 알림. 그래서 받자마자 다시 읽으면 반영된 점수가 옴
  useEffect(() => {
    if (finished) {
      queryClient.invalidateQueries({ queryKey: QUERY.CURRENT_USER.queryKey });
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
      queryClient.invalidateQueries({ queryKey: QUERY.RANKS.queryKey });
    }
  }, [finished, queryClient]);

  // 판 도중에 본 점수가 시작 점수. 점수는 판이 끝날 때만 바뀌므로 새로고침해 들어와도 같음
  if (status === 'PLAYING' && currentUser && mmrBefore === null) {
    setMmrBefore(currentUser.mmr);
  }
  // 게스트가 낀 판은 점수가 움직이지 않음
  const rating =
    !room?.rated || !currentUser || currentUser.visitor || mmrBefore === null
      ? null
      : fetchingUser
        ? 'PENDING'
        : { before: mmrBefore, after: currentUser.mmr };

  return (
    <main
      className={`transition-removing relative flex h-full grow flex-col justify-between overflow-hidden p-4 transition-colors duration-1000 md:gap-4 ${gameState.awaitingRemoval && 'bg-red-200'}`}
    >
      <ResignModal
        visible={showResignModal}
        rated={room?.rated ?? false}
        onResign={onResign}
        onClose={() => setShowResignModal(false)}
      />
      {/* 내 정보가 오기 전에 판정하면 승자·패자 비교가 둘 다 거짓이라 무승부로 보임 */}
      {currentUser && (
        <GameResultModal
          visible={finished}
          result={resultOf(gameState, currentUser.userId)}
          rating={rating}
          endReason={gameState.endReason}
          notice={endNotice}
          onLeaveRoom={onLeaveRoom}
        />
      )}
      <HelpModal
        visible={showHelpModal}
        onClose={() => setShowHelpModal(false)}
      />
      <RequestDrawModal
        visible={showRequestDrawModal}
        onRequestDraw={onOfferDraw}
        onClose={() => setShowRequestDrawModal(false)}
      />
      <ResponseDrawModal
        visible={showResponseDrawModal}
        onAcceptDraw={onAcceptDraw}
        onRejectDraw={onDeclineDraw}
      />
      <DrawRejectedModal
        visible={showDrawRejectedModal}
        onClose={() => setShowDrawRejectedModal(false)}
      />
      <SocketErrorModal visible={disconnected} onLeaveRoom={onLeaveRoom} />
      <AlertModal
        message={
          // 두던 판이 사라진 것과 처음부터 없는 방을 연 것은 다른 상황
          !inGame && (roomGone || roomLost)
            ? '사라진 방입니다'
            : roomLost
              ? '서버가 다시 시작됐거나 오래 비어 있어 방이 사라졌습니다'
              : ''
        }
        onClose={onLeaveRoom}
      />
      {!inGame ? (
        <WaitingRoom
          room={room}
          isHost={isHost}
          onChangeFirstMoveRule={onChangeFirstMoveRule}
          onStart={onStart}
          onLeave={onLeaveRoom}
        />
      ) : (
        <>
          <div className="flex flex-col items-center justify-between gap-8 md:flex-row md:items-start">
            {/* key 가 바뀌면 다시 마운트돼 단계가 넘어갈 때마다 깜빡임이 재생됨 */}
            <PhaseBanner key={getPlayerPhase()} phase={getPlayerPhase()} />
            <Status
              turn={gameState.status === 'PLAYING' && !isPlayerTurn()}
              secondsLeft={secondsLeft}
              color={enemyStone()}
              inHand={getEnemyInHand()}
              onBoard={getEnemyOnBoard()}
              player={enemy}
            />
          </div>
          {/* 좁은 화면은 위아래 정보와 겹치지 않게 흐름에 넣음
              넓은 화면은 정보가 모서리에 있어 가운데 겹쳐도 부딪히지 않고, 흐름에 넣으면 세로가 모자람 */}
          <div className="flex grow items-center justify-center md:absolute md:inset-0">
            <Board
              client={client}
              board={gameState.board}
              selectable={getPlayerPhase() !== 'PLACING' && isPlayerTurn()}
              placeable={getPlayerPhase() === 'PLACING' && isPlayerTurn()}
              removableStone={
                gameState.awaitingRemoval && isPlayerTurn()
                  ? enemyStone()
                  : 'EMPTY'
              }
              lastPoint={lastPoint}
              motion={motion}
              millFormed={gameState.awaitingRemoval}
              playerStoneColor={myStone()}
              addStone={addStone}
              moveStone={moveStone}
              removeStone={removeStone}
              onReject={onReject}
            />
          </div>
          <div className="flex w-full flex-col items-center justify-between md:flex-row-reverse md:items-end">
            <Message
              phase={getPlayerPhase()}
              removing={gameState.awaitingRemoval}
              error={error}
              turn={gameState.status === 'PLAYING' && isPlayerTurn()}
            />
            <Status
              isCurrentUser
              turn={gameState.status === 'PLAYING' && isPlayerTurn()}
              secondsLeft={secondsLeft}
              color={myStone()}
              inHand={getPlayerInHand()}
              onBoard={getPlayerOnBoard()}
              player={currentUser}
              onShowResignModal={() => setShowResignModal(true)}
              onShowRequestDrawModal={() => setShowRequestDrawModal(true)}
              onShowHelpModal={() => setShowHelpModal(true)}
            />
          </div>
        </>
      )}
    </main>
  );
}

// 이번 수로 새로 생긴 돌의 자리. 이동이면 도착 지점, 제거만 했으면 -1
function addedPoint(before: string[], after: string[]) {
  return after.findIndex(
    (stone, idx) => stone !== 'EMPTY' && before[idx] === 'EMPTY'
  );
}

function removedPoint(before: string[], after: string[]) {
  return after.findIndex(
    (stone, idx) => stone === 'EMPTY' && before[idx] !== 'EMPTY'
  );
}

function PhaseBanner({ phase }: { phase: 'PLACING' | 'MOVING' | 'FLYING' }) {
  const label = {
    PLACING: { title: 'Phase 1', description: '돌 배치 단계' },
    MOVING: { title: 'Phase 2', description: '돌 이동 단계' },
    FLYING: { title: 'Phase 3', description: '자유 이동 단계' },
  }[phase];

  return (
    <div className="z-20 flex w-full animate-blinking items-center justify-center gap-4 bg-phase text-white md:flex-col md:items-start md:gap-0 md:bg-none md:text-black">
      <h1 className="font-phase text-xl md:text-6xl">{label.title}</h1>
      <span className="text-lg font-semibold">{label.description}</span>
    </div>
  );
}
