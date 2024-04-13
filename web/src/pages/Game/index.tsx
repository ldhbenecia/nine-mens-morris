import { useEffect, useState } from 'react';
import { Board } from './Board';
import { Status } from './Status';
import { WithdrawModal } from './WithdrawModal';
import { useGameState } from '~/hooks/useGameState';
import { Client } from '@stomp/stompjs';
import { useParams } from 'react-router-dom';
import { QUERY } from '~/lib/queries';
import { useQuery } from '@tanstack/react-query';

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
  const { gameState, ...methods } = useGameState();

  const onWithdraw = () => {
    setShowModal(true);
  };

  useEffect(() => {
    client.onConnect = function (frame) {
      console.log('연결됨', frame);

      client.subscribe(`/topic/game/${roomId}`, (message) => {
        console.log('수신: ', message.body);
      });
    };

    client.onStompError = function (frame) {
      console.log('Broker reported error: ' + frame.headers['message']);
      console.log('Additional details: ' + frame.body);
    };

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [roomId]);

  return (
    <main className="flex grow flex-col overflow-x-hidden p-4 md:gap-4">
      {
        <WithdrawModal
          isShowing={showModal}
          closeModal={() => setShowModal(false)}
        />
      }
      <div className="flex flex-col items-center justify-between gap-8 md:flex-row md:items-start">
        <div className="z-20 flex w-full justify-center gap-4 bg-phase text-white md:flex-col md:gap-0 md:bg-none md:text-black">
          <h1 className="font-phase text-xl md:text-5xl">Phase 1</h1>
          <span className="font-semibold">돌 배치 단계</span>
        </div>
        <Status
          isCurrentUser={false}
          isTurn={false}
          color="BLACK"
          remaining={5}
        />
      </div>
      <Board board={gameState.board} />
      <div className="flex w-full flex-col items-center justify-between md:flex-row-reverse md:items-end">
        <div className="flex animate-pulse py-2">
          빈 지점에 돌을 배치하세요.
        </div>
        <Status
          isCurrentUser={true}
          isTurn={true}
          color="WHITE"
          remaining={7}
          onWithdraw={onWithdraw}
        />
      </div>
    </main>
  );
}
