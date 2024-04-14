import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import { useGameState } from '~/hooks/useGameState';
import { Button } from '~/components';
import Logout from '~/assets/icons/logout.svg?react';
import { useLeaveRoom } from '~/hooks/useMutations';
import { Board } from './Board';
import { Status } from './Status';
import { WithdrawModal } from './WithdrawModal';
import { useQuery } from '@tanstack/react-query';
import { QUERY } from '~/lib/queries';

const client = new Client({
  brokerURL: 'ws://localhost:8080/morris-websocket',
  debug: console.log,
  reconnectDelay: 5000,
  heartbeatIncoming: 4000,
  heartbeatOutgoing: 4000,
});

export function GamePage() {
  const [showModal, setShowModal] = useState(false);
  const { roomId } = useParams();
  const { data: currentUser } = useQuery(QUERY.CURRENT_USER);
  const [connected, setConnected] = useState(client.connected);
  const { mutate: leaveRoom } = useLeaveRoom();
  const {
    gameState,
    setGameState,
    addStone,
    moveStone,
    removeStone,
    isPlayerHost,
    isPlayerTurn,
    getPlayerStoneColor,
    getEnemyStoneColor,
  } = useGameState();

  const onShowWithdrawModal = () => {
    setShowModal(true);
  };

  const onLeaveRoom = () => {
    if (roomId) {
      leaveRoom(Number(roomId));
    }
  };

  const sendMessage = () => {
    client.publish({
      destination: `/topic/test`,
      body: JSON.stringify('안뇽'),
    });
  };

  const handleEvent = useCallback(
    (body: string) => {
      const response = JSON.parse(body);
      switch (response.type) {
        case 'SYNC_GAME':
          console.log('전달된 상태:', response.state);
          setGameState(response.state);
          break;
      }
    },
    [setGameState]
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
    if (connected) {
      client.publish({ destination: `/app/joinGame/${roomId}` });
    }
  }, [connected, roomId, currentUser]);

  useEffect(() => {
    client.onConnect = () => {
      console.log('소켓에 연결되었습니다.');

      client.subscribe(`/topic/test`, (message) => {
        console.log('테스트 수신: ', message.body);
      });

      client.subscribe(`/topic/game/${roomId}`, (message) => {
        handleEvent(message.body);
      });

      setConnected(true);
    };
  }, [roomId, handleEvent]);

  if (!gameState) {
    return <div>로딩 중...</div>;
  }

  return (
    <main
      className={`transition-removing flex grow flex-col overflow-x-hidden transition-colors duration-1000 ${gameState.isRemoving && 'bg-red-200'} p-4 md:gap-4`}
    >
      {
        <WithdrawModal
          isShowing={showModal}
          onLeaveRoom={onLeaveRoom}
          closeModal={() => setShowModal(false)}
        />
      }
      <div className="flex flex-col items-center justify-between gap-8 md:flex-row md:items-start">
        {gameState.status === 'WAITING' ? (
          <div className="flex items-center gap-2">
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
          <div className="z-20 flex w-full items-center justify-center gap-4 bg-phase text-white md:flex-col md:items-start md:gap-0 md:bg-none md:text-black">
            <h1 className="font-phase text-xl md:text-5xl">
              Phase {gameState.phase}
            </h1>
            <span className="font-semibold">
              돌 {gameState.phase === 1 ? '배치' : '이동'} 단계
            </span>
          </div>
        )}
        {
          <Status
            isTurn={!isPlayerTurn()}
            color={getEnemyStoneColor()}
            addable={
              !isPlayerHost() ? gameState.hostAddable : gameState.guestAddable
            }
            total={!isPlayerHost() ? gameState.hostTotal : gameState.guestTotal}
            visible={gameState.hostId !== -1}
          />
        }
      </div>
      {client && (
        <Board
          client={client}
          board={gameState.board}
          playerStoneColor={getPlayerStoneColor()}
          addStone={addStone}
          removeStone={removeStone}
        />
      )}
      <div className="flex w-full flex-col items-center justify-between md:flex-row-reverse md:items-end">
        <div
          className={`flex animate-pulse py-2 ${isPlayerTurn() ? 'visible' : 'invisible'}  ${gameState.isRemoving ? 'text-red-800' : ''}`}
          onClick={sendMessage}
        >
          {gameState.isRemoving
            ? '상대의 돌 중 하나를 선택해 제거하세요.'
            : gameState.phase === 1
              ? '빈 지점에 돌을 배치하세요.'
              : '돌을 인접한 지점으로 옮길 수 있습니다.'}
        </div>
        <Status
          isCurrentUser
          isTurn={isPlayerTurn()}
          color={getPlayerStoneColor()}
          addable={
            isPlayerHost() ? gameState.hostAddable : gameState.guestAddable
          }
          total={isPlayerHost() ? gameState.hostTotal : gameState.guestTotal}
          onShowWithdrawModal={onShowWithdrawModal}
        />
      </div>
    </main>
  );
}
