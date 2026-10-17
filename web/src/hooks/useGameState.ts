import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { QUERY } from '~/lib/queries';
import { Client } from '@stomp/stompjs';
import { FirstMoveRule, GameState, StoneType } from '~/lib/types';
import { clickSound } from '~/lib/sounds';
import { DESTINATION, RoomAction } from '~/lib/socket';

const initialGameState: GameState = {
  board: [...Array(24)].map(() => 'EMPTY'),
  blackId: -1,
  whiteId: -1,
  currentTurnId: -1,
  blackInHand: 9,
  whiteInHand: 9,
  blackOnBoard: 0,
  whiteOnBoard: 0,
  blackPhase: 'PLACING',
  whitePhase: 'PLACING',
  awaitingRemoval: false,
  status: 'WAITING',
  winnerId: null,
  loserId: null,
  endReason: null,
  turnRemainingMillis: null,
  revision: -1,
};

let errorTimerId = 0;

// 규칙 판단은 하지 않음. 서버가 판정하고 거절 사유를 개인 큐로 보내줌
// 여기서 턴·빈 칸·단계를 미리 막으면 서버에 가지 않아 거절 문구도 효과음도 없이 무반응이 됨
// 끝난 판처럼 보낼 이유가 아예 없는 것만 막음
export function useGameState(client: Client, roomId: number) {
  const [gameState, setGameState] = useState<GameState>(initialGameState);
  const [error, setError] = useState('');
  const { data: currentUser } = useQuery(QUERY.CURRENT_USER);

  const myStone: StoneType =
    currentUser?.userId === gameState.blackId
      ? 'BLACK'
      : currentUser?.userId === gameState.whiteId
        ? 'WHITE'
        : 'EMPTY';
  const enemyStone: StoneType =
    myStone === 'BLACK' ? 'WHITE' : myStone === 'WHITE' ? 'BLACK' : 'EMPTY';
  const black = myStone === 'BLACK';
  const gameOver = gameState.status === 'FINISHED';
  const playerTurn =
    !!currentUser && gameState.currentTurnId === currentUser.userId;

  const player = {
    inHand: black ? gameState.blackInHand : gameState.whiteInHand,
    onBoard: black ? gameState.blackOnBoard : gameState.whiteOnBoard,
    phase: black ? gameState.blackPhase : gameState.whitePhase,
  };
  const enemy = {
    id: black ? gameState.whiteId : gameState.blackId,
    inHand: black ? gameState.whiteInHand : gameState.blackInHand,
    onBoard: black ? gameState.whiteOnBoard : gameState.blackOnBoard,
  };

  const showError = (message: string) => {
    if (errorTimerId) {
      clearTimeout(errorTimerId);
    }

    setError(message);
    errorTimerId = window.setTimeout(() => setError(''), 3000);
  };

  const send = (action: RoomAction, body?: object) => {
    client.publish({
      destination: DESTINATION.roomAction(roomId, action),
      body: body ? JSON.stringify(body) : undefined,
    });
  };

  // 끝난 판에 보내는 수는 서버가 거절할 뿐이라 보내지 않음
  const play = (action: RoomAction, body?: object) => {
    if (!gameOver) send(action, body);
  };

  return {
    gameState,
    error,
    setGameState,
    showError,
    playerTurn,
    myStone,
    enemyStone,
    player,
    enemy,
    place: (point: number) => play('place', { to: point }),
    move: (from: number, to: number) => play('move', { from, to }),
    // 3연속 보호 여부도 서버가 판단함. 상대 돌이 전부 3연속이면 그마저 제거 가능한 예외가 있음
    remove: (point: number) => {
      play('remove', { at: point });
      setError('');
    },
    resign: () => play('resign'),
    offerDraw: () => play('draw-offer'),
    acceptDraw: () => play('draw-accept'),
    declineDraw: () => play('draw-decline'),
    start: () => {
      clickSound.play();
      send('start');
    },
    changeFirstMoveRule: (rule: FirstMoveRule) =>
      send('settings', { firstMoveRule: rule }),
  };
}
