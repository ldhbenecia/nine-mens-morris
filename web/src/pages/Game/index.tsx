import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Client } from '@stomp/stompjs';
import { NoRoomAlert } from '~/components';
import { QUERY } from '~/lib/queries';
import { useGameState, useLeaveRoom } from '~/hooks';
import { FirstMoveRule, MoveRejected, RoomEvent } from '~/lib/types';
import { Board } from './Board';
import { WaitingRoom } from './WaitingRoom';
import { Status } from './Status';
import { WithdrawModal } from './WithdrawModal';
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
  notificationSound,
} from '~/lib/sounds';
import { DrawRejectedModal } from './DrawRejectedModal';
import { SocketErrorModal } from './SocketErrorModal';

const client = new Client({
  brokerURL: import.meta.env.VITE_SOCKET_URL,
  reconnectDelay: 5000,
  heartbeatIncoming: 4000,
  heartbeatOutgoing: 4000,
});

export function GamePage() {
  const { roomId } = useParams();
  const [showWithdrawModal, setShowWithdrawModal] = useState(false);
  const [showRequestDrawModal, setShowRequestDrawModal] = useState(false);
  const [showResponseDrawModal, setShowResponseDrawModal] = useState(false);
  const [showDrawRejectedModal, setShowDrawRejectedModal] = useState(false);
  const [showHelpModal, setShowHelpModal] = useState(false);
  const [showGameResultModal, setShowGameResultModal] = useState(false);
  const [endNotice, setEndNotice] = useState('');
  const [disconnected, setDisconnected] = useState(false);
  const { data: currentUser } = useQuery(QUERY.CURRENT_USER);
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
    ...QUERY.USER_NICKNAME(enemyId()),
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
    setShowWithdrawModal(false);
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

  const onStart = () => {
    if (roomId) startGame(client, Number(roomId));
  };

  // 착수 효과음. 예전에는 클라이언트가 /topic 으로 직접 발행해 서로에게 알렸는데
  // 서버가 그 경로를 막았고, 애초에 착수 결과가 브로드캐스트되므로 필요 없다
  const playMoveSound = useCallback((before: string[], after: string[]) => {
    const added = after.findIndex(
      (stone, idx) => stone !== 'EMPTY' && before[idx] === 'EMPTY'
    );
    const removed = after.findIndex(
      (stone, idx) => stone === 'EMPTY' && before[idx] !== 'EMPTY'
    );

    if (removed !== -1 && added === -1) {
      stoneDroppingSound.currentTime = 0;
      stoneDroppingSound.play();
      return;
    }
    if (added === -1) return;

    const sound = after[added] === 'BLACK' ? blackStoneSound : whiteStoneSound;
    sound.currentTime = 0;
    sound.play();
  }, []);

  const handleEvent = useCallback(
    (event: RoomEvent) => {
      switch (event.type) {
        case 'STARTED':
          if (event.state) setGameState(event.state);
          startSound.play();
          break;
        case 'SNAPSHOT':
          if (event.state) setGameState(event.state);
          break;
        case 'STATE_CHANGED':
          if (!event.state) break;
          setGameState((previous) => {
            playMoveSound(previous.board, event.state!.board);
            return event.state!;
          });
          break;
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
    },
    [currentUser, playMoveSound, setGameState, refetchRoom]
  );

  // onConnect 는 한 번만 불리는데 구독 콜백은 그때의 handleEvent 를 영구히 붙잡는다
  // 그 안의 currentUser 가 아직 undefined 이면, 무승부를 신청한 사람이
  // 자기 신청 모달을 받는 식으로 어긋난다. ref 로 항상 최신 것을 부른다
  const handleEventRef = useRef(handleEvent);
  handleEventRef.current = handleEvent;
  const showErrorRef = useRef(showError);
  showErrorRef.current = showError;

  useEffect(() => {
    if (!roomId) return;

    client.onConnect = () => {
      setDisconnected(false);

      // 방 토픽 하나만 구독하면 된다. 예전에는 토픽이 둘로 나뉘어 있었고
      // 그중 하나는 아무도 구독하지 않았다
      client.subscribe(`/topic/rooms/${roomId}`, (message) => {
        handleEventRef.current(JSON.parse(message.body) as RoomEvent);
      });

      // 규칙 위반 거절과 권한 오류는 방이 아니라 나에게만 온다
      client.subscribe('/user/queue/errors', (message) => {
        const rejected = JSON.parse(message.body) as MoveRejected;
        showErrorRef.current(rejected.message);
        explosionSound.currentTime = 0;
        explosionSound.play();
      });

      // 새로고침 후 판 복구
      client.subscribe('/user/queue/sync', (message) => {
        handleEventRef.current(JSON.parse(message.body) as RoomEvent);
      });

      // 구독 직후 진행 중인 판이 있는지 물어본다. 새로고침해도 판을 잃지 않는다
      client.publish({ destination: `/app/rooms/${roomId}/sync` });
      joinSound.play();
    };

    // 끊긴 상태에서 publish 하면 stompjs 가 throw 하므로 화면에 알려야 한다
    client.onWebSocketClose = () => setDisconnected(true);
    client.onStompError = (frame) =>
      showErrorRef.current(
        frame.headers.message ?? '서버와 통신에 실패했습니다.'
      );

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

  // 어떤 이벤트로 끝났든 결과 화면은 떠야 한다
  // 예전에는 FINISHED 이벤트에서만 열어서, 끝난 판에 새로고침해 SNAPSHOT 으로
  // 복구되면 결과도 나가기 버튼도 없는 화면에 갇혔다
  const { status, winnerId, loserId } = gameState;
  useEffect(() => {
    if (status !== 'FINISHED') return;
    setShowGameResultModal(true);
    if (!currentUser) return;
    if (winnerId === currentUser.userId) winSound.play();
    if (loserId === currentUser.userId) lossSound.play();
  }, [status, winnerId, loserId, currentUser]);

  return (
    <main
      className={`transition-removing flex h-full grow flex-col justify-between overflow-hidden p-4 transition-colors duration-1000 md:gap-4 ${gameState.awaitingRemoval && 'bg-red-200'}`}
    >
      <WithdrawModal
        visible={showWithdrawModal}
        onWithdraw={onResign}
        onClose={() => setShowWithdrawModal(false)}
      />
      <GameResultModal
        visible={showGameResultModal}
        result={
          gameState.winnerId === currentUser?.userId
            ? 'WIN'
            : gameState.loserId === currentUser?.userId
              ? 'LOSS'
              : 'DRAW'
        }
        notice={endNotice}
        onLeaveRoom={onLeaveRoom}
      />
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
      <NoRoomAlert visible={roomGone && !inGame} onClose={onLeaveRoom} />
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
            <PhaseBanner phase={getPlayerPhase()} />
            <Status
              turn={!isPlayerTurn()}
              color={enemyStone()}
              inHand={getEnemyInHand()}
              onBoard={getEnemyOnBoard()}
              nickname={enemy?.nickname}
            />
          </div>
          <Board
            client={client}
            board={gameState.board}
            selectable={getPlayerPhase() !== 'PLACING' && isPlayerTurn()}
            playerStoneColor={myStone()}
            addStone={addStone}
            moveStone={moveStone}
            removeStone={removeStone}
          />
          <div className="flex w-full flex-col items-center justify-between md:flex-row-reverse md:items-end">
            <Message
              phase={getPlayerPhase()}
              removing={gameState.awaitingRemoval}
              error={error}
              turn={isPlayerTurn()}
            />
            <Status
              isCurrentUser
              turn={isPlayerTurn()}
              color={myStone()}
              inHand={getPlayerInHand()}
              onBoard={getPlayerOnBoard()}
              nickname={currentUser?.nickname}
              onShowWithdrawModal={() => setShowWithdrawModal(true)}
              onShowRequestDrawModal={() => setShowRequestDrawModal(true)}
              onShowHelpModal={() => setShowHelpModal(true)}
            />
          </div>
        </>
      )}
    </main>
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
