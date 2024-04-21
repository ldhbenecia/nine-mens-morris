import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Client } from '@stomp/stompjs';
import { QUERY } from '~/lib/queries';
import Logout from '~/assets/icons/logout.svg?react';
import { useGameState, useLeaveRoom } from '~/hooks';
import { Button } from '~/components';
import { Board } from './Board';
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
  debug: console.log,
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
  const [showSocketErrorModal, setShowSocketErrorModal] = useState(false);
  const drawRequesterRef = useRef(-1);
  const [connected, setConnected] = useState(client.connected);
  const { data: currentUser } = useQuery(QUERY.CURRENT_USER);
  const { mutate: leaveRoom } = useLeaveRoom();
  const {
    gameState,
    error,
    enemyNickname,
    setGameState,
    isPlayerTurn,
    isPlayerFlyMode,
    isGameOver,
    getPlayerStoneColor,
    getEnemyStoneColor,
    getPlayerAddable,
    getEnemyAddable,
    getPlayerTotal,
    getEnemyTotal,
    addStone,
    moveStone,
    removeStone,
    skipRemoving,
    withdraw,
    requestDraw,
    acceptDraw,
    rejectDraw,
  } = useGameState();

  const onLeaveRoom = () => {
    if (roomId) {
      leaveRoom(Number(roomId));
    }
  };

  const onWithdraw = () => {
    if (roomId) {
      withdraw(client, Number(roomId));
    }
  };

  const onRequestDraw = () => {
    if (roomId && currentUser) {
      requestDraw(client, Number(roomId));
      drawRequesterRef.current = currentUser.userId;
    }

    setShowRequestDrawModal(false);
  };

  const onAcceptDraw = () => {
    if (roomId) {
      acceptDraw(client, Number(roomId));
    }

    setShowResponseDrawModal(false);
  };

  const onRejectDraw = () => {
    if (roomId) {
      rejectDraw(client, Number(roomId));
    }

    setShowResponseDrawModal(false);
  };

  const onSkipRemoving = () => {
    if (roomId) {
      skipRemoving(client, Number(roomId));
    }
  };

  const handleClientEvent = useCallback((contents: string) => {
    switch (contents) {
      case 'ADD_WHITE':
        whiteStoneSound.currentTime = 0;
        whiteStoneSound.play();
        break;
      case 'ADD_BLACK':
        blackStoneSound.currentTime = 0;
        blackStoneSound.play();
        break;
      case 'REMOVE_STONE':
        stoneDroppingSound.currentTime = 0;
        stoneDroppingSound.play();
        break;
    }
  }, []);

  const handleEvent = useCallback(
    (body: string) => {
      const response = JSON.parse(body);
      console.log(response);
      if (response === 'SOCKET_ERROR') {
        setShowSocketErrorModal(true);
        return;
      }

      switch (response.type) {
        case 'CLIENT_EVENT':
          handleClientEvent(response.contents);
          break;
        case 'GAME_START':
          setGameState(response.data);
          startSound.play();
          break;
        case 'GAME_OVER':
        case 'GAME_WITHDRAW':
          setGameState(response.data);
          setShowGameResultModal(true);
          break;
        case 'REQUEST_DRAW':
          if (currentUser?.userId !== drawRequesterRef.current) {
            setShowResponseDrawModal(true);
          }
          break;
        case 'REJECT_DRAW':
          if (currentUser?.userId === drawRequesterRef.current) {
            setShowDrawRejectedModal(true);
          }

          drawRequesterRef.current = -1;
          break;
        case 'GAME_DRAW':
          notificationSound.play();
          setShowGameResultModal(true);
          break;
        default:
          setGameState(response.data);
      }
    },
    [setGameState, handleClientEvent, currentUser]
  );

  // 마운트/언마운트 시 소켓 연결/종료
  useEffect(() => {
    client.activate();

    return () => {
      client.deactivate();
    };
  }, []);

  // 최초 연결 시 입장 이벤트 전송
  useEffect(() => {
    if (connected && roomId && currentUser) {
      client.publish({
        destination: `/app/joinGame/${roomId}`,
        body: JSON.stringify(currentUser.userId),
      });
    }
  }, [connected, roomId, currentUser]);

  useEffect(() => {
    client.onConnect = () => {
      client.subscribe(`/topic/game/${roomId}`, (message) => {
        handleEvent(message.body);
      });

      setConnected(true);
      joinSound.play();
    };
  }, [roomId, handleEvent]);

  useEffect(() => {
    if (gameState.removing) {
      explosionSound.play();
    }

    if (isGameOver() && currentUser) {
      if (gameState.winner === currentUser.userId) {
        winSound.play();
      }

      if (gameState.loser === currentUser.userId) {
        lossSound.play();
      }
    }
  }, [gameState, currentUser, isGameOver, gameState.removing]);

  if (!gameState) {
    return <div>로딩 중...</div>;
  }

  return (
    <main
      className={`transition-removing flex h-full grow flex-col justify-between overflow-hidden p-4 transition-colors  duration-1000 md:gap-4 ${gameState.removing && 'bg-red-200'}`}
    >
      <WithdrawModal
        visible={showWithdrawModal}
        onWithdraw={onWithdraw}
        onClose={() => setShowWithdrawModal(false)}
      />
      <GameResultModal
        visible={showGameResultModal}
        result={
          gameState.winner === currentUser?.userId
            ? 'WIN'
            : gameState.loser === currentUser?.userId
              ? 'LOSS'
              : 'DRAW'
        }
        onLeaveRoom={onLeaveRoom}
      />
      <HelpModal
        visible={showHelpModal}
        onClose={() => setShowHelpModal(false)}
      />
      <RequestDrawModal
        visible={showRequestDrawModal}
        onRequestDraw={onRequestDraw}
        onClose={() => setShowRequestDrawModal(false)}
      />
      <ResponseDrawModal
        visible={showResponseDrawModal}
        onAcceptDraw={onAcceptDraw}
        onRejectDraw={onRejectDraw}
      />
      <DrawRejectedModal
        visible={showDrawRejectedModal}
        onClose={() => setShowDrawRejectedModal(false)}
      />
      <SocketErrorModal
        visible={showSocketErrorModal}
        onLeaveRoom={onLeaveRoom}
      />
      <div className="flex flex-col items-center justify-between gap-8 md:flex-row md:items-start">
        {gameState.status === 'WAITING' ? (
          <div className="z-10 flex items-center gap-2">
            <span className="animate-pulse">상대를 기다리는 중...</span>
            <Button
              slim
              text="나가기"
              theme="secondary"
              icon={<Logout />}
              onClick={onLeaveRoom}
            />
          </div>
        ) : (
          <>
            {gameState.phase === 1 && (
              <div className="z-20 flex w-full animate-blinking items-center justify-center gap-4 bg-phase text-white md:flex-col md:items-start md:gap-0 md:bg-none md:text-black">
                <h1 className="font-phase text-xl md:text-6xl">Phase 1</h1>
                <span className="text-lg font-semibold">돌 배치 단계</span>
              </div>
            )}
            {gameState.phase === 2 && (
              <div className="z-20 flex w-full animate-blinking items-center justify-center gap-4 bg-phase text-white md:flex-col md:items-start md:gap-0 md:bg-none md:text-black">
                <h1 className="font-phase text-xl md:text-6xl">Phase 2</h1>
                <span className="text-lg font-semibold">돌 이동 단계</span>
              </div>
            )}
          </>
        )}
        <Status
          turn={!isPlayerTurn()}
          color={getEnemyStoneColor()}
          addable={getEnemyAddable()}
          total={getEnemyTotal()}
          nickname={enemyNickname}
          visible={gameState.status !== 'WAITING'}
        />
      </div>
      {client && (
        <Board
          client={client}
          board={gameState.board}
          selectable={gameState.phase === 2 && isPlayerTurn()}
          playerStoneColor={getPlayerStoneColor()}
          addStone={addStone}
          moveStone={moveStone}
          removeStone={removeStone}
        />
      )}
      <div className="flex w-full flex-col items-center justify-between md:flex-row-reverse md:items-end">
        <Message
          phase={gameState.phase}
          removing={gameState.removing}
          error={error}
          turn={isPlayerTurn()}
          onSkipRemoving={onSkipRemoving}
          flying={isPlayerFlyMode()}
        />
        <Status
          isCurrentUser
          turn={isPlayerTurn()}
          color={getPlayerStoneColor()}
          addable={getPlayerAddable()}
          total={getPlayerTotal()}
          nickname={currentUser?.nickname}
          onShowWithdrawModal={() => setShowWithdrawModal(true)}
          onShowRequestDrawModal={() => setShowRequestDrawModal(true)}
          onShowHelpModal={() => setShowHelpModal(true)}
        />
      </div>
    </main>
  );
}
