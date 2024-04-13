import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import { useGameState } from '~/hooks/useGameState';
import { GameState } from '~/lib/types';
import { Button } from '~/components';
import Logout from '~/assets/icons/logout.svg?react';
import { Board } from './Board';
import { Status } from './Status';
import { WithdrawModal } from './WithdrawModal';

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
  const { gameState, ...gameMethods } = useGameState();

  const onWithdraw = () => {
    setShowModal(true);
  };

  const sendMessage = () =>
    client.publish({
      destination: `/topic/game/test/${roomId}`,
      body: JSON.stringify('안뇽'),
    });

  useEffect(() => {
    client.onConnect = function (frame) {
      console.log('연결됨', frame);

      client.subscribe(`/topic/game/test/${roomId}`, (message) => {
        console.log('테스트 수신: ', message.body);
      });

      client.subscribe(`/topic/game/${roomId}`, (message) => {
        console.log('수신: ', message.body);
        const newGameState: GameState = JSON.parse(message.body);
        gameMethods.updateGameState(newGameState);
      });

      // 방 참가 이벤트 전송
      client.publish({ destination: `/app/joinGame/${roomId}` });
    };

    client.onStompError = function (frame) {
      console.log('Broker reported error: ' + frame.headers['message']);
      console.log('Additional details: ' + frame.body);
    };

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [roomId, gameMethods]);

  if (!gameState) {
    return <div>로딩 중...</div>;
  }

  return (
    <main className="flex grow flex-col overflow-x-hidden p-4 md:gap-4">
      {
        <WithdrawModal
          isShowing={showModal}
          closeModal={() => setShowModal(false)}
        />
      }
      <div className="flex flex-col items-center justify-between gap-8 md:flex-row md:items-start">
        {gameState.status === 'WAITING' ? (
          <div className="flex items-center gap-2">
            <span className="animate-pulse">상대를 기다리는 중...</span>
            <Button slim text="나가기" theme="secondary" icon={<Logout />} />
          </div>
        ) : (
          <div className="z-20 flex w-full items-center justify-center gap-4 bg-phase text-white md:flex-col md:gap-0 md:bg-none md:text-black">
            <h1 className="font-phase text-xl md:text-5xl">
              Phase {gameState.phase}
            </h1>
            <span className="font-semibold">
              돌 {gameState.phase === 1 ? '배치' : '이동'} 단계
            </span>
          </div>
        )}
        <Status
          isTurn={false}
          color={gameMethods.getEnemyStoneColor()}
          remaining={gameMethods.countEnemyAddableStone()}
        />
      </div>
      <Board
        client={client}
        board={gameState.board}
        addStone={gameMethods.addStone}
      />
      <div className="flex w-full flex-col items-center justify-between md:flex-row-reverse md:items-end">
        <div className="flex animate-pulse py-2" onClick={sendMessage}>
          빈 지점에 돌을 배치하세요.
        </div>
        <Status
          isCurrentUser
          isTurn={gameMethods.isPlayerTurn()}
          color={gameMethods.getPlayerStoneColor()}
          remaining={gameMethods.countPlayerAddableStone()}
          onWithdraw={onWithdraw}
        />
      </div>
    </main>
  );
}
