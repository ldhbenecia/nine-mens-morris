import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { QUERY } from '~/lib/queries';
import { Client } from '@stomp/stompjs';
import { GameState, StoneType } from '~/lib/types';
import { clickSound } from '~/lib/sounds';

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
export function useGameState() {
  const [gameState, setGameState] = useState<GameState>(initialGameState);
  const [error, setError] = useState('');
  const { data: currentUser } = useQuery(QUERY.CURRENT_USER);

  const myStone = (): StoneType => {
    if (!currentUser) return 'EMPTY';
    if (gameState.blackId === currentUser.userId) return 'BLACK';
    if (gameState.whiteId === currentUser.userId) return 'WHITE';
    return 'EMPTY';
  };

  const enemyStone = (): StoneType => {
    const mine = myStone();
    if (mine === 'BLACK') return 'WHITE';
    if (mine === 'WHITE') return 'BLACK';
    return 'EMPTY';
  };

  const isBlack = () => myStone() === 'BLACK';

  const isGameOver = () => gameState.status === 'FINISHED';

  const isPlayerTurn = () =>
    !!currentUser && gameState.currentTurnId === currentUser.userId;

  const getPlayerInHand = () =>
    isBlack() ? gameState.blackInHand : gameState.whiteInHand;

  const getEnemyInHand = () =>
    isBlack() ? gameState.whiteInHand : gameState.blackInHand;

  const getPlayerOnBoard = () =>
    isBlack() ? gameState.blackOnBoard : gameState.whiteOnBoard;

  const getEnemyOnBoard = () =>
    isBlack() ? gameState.whiteOnBoard : gameState.blackOnBoard;

  const getPlayerPhase = () =>
    isBlack() ? gameState.blackPhase : gameState.whitePhase;

  const enemyId = () => (isBlack() ? gameState.whiteId : gameState.blackId);

  const showError = (message: string) => {
    if (errorTimerId) {
      clearTimeout(errorTimerId);
    }

    setError(message);
    errorTimerId = window.setTimeout(() => setError(''), 3000);
  };

  const send = (
    client: Client,
    roomId: number,
    action: string,
    body?: object
  ) => {
    client.publish({
      destination: `/app/rooms/${roomId}/${action}`,
      body: body ? JSON.stringify(body) : undefined,
    });
  };

  const addStone = (client: Client, roomId: number, index: number) => {
    if (!isGameOver()) send(client, roomId, 'place', { to: index });
  };

  const moveStone = (
    client: Client,
    roomId: number,
    from: number,
    to: number
  ) => {
    if (!isGameOver()) send(client, roomId, 'move', { from, to });
  };

  // 3연속 보호 여부도 서버가 판단함. 상대 돌이 전부 3연속이면 그마저 제거 가능한 예외가 있음
  const removeStone = (client: Client, roomId: number, index: number) => {
    if (isGameOver()) return;
    send(client, roomId, 'remove', { at: index });
    setError('');
  };

  const resign = (client: Client, roomId: number) => {
    if (!isGameOver()) send(client, roomId, 'resign');
  };

  const offerDraw = (client: Client, roomId: number) => {
    if (!isGameOver()) send(client, roomId, 'draw-offer');
  };

  const acceptDraw = (client: Client, roomId: number) => {
    if (!isGameOver()) send(client, roomId, 'draw-accept');
  };

  const declineDraw = (client: Client, roomId: number) => {
    if (!isGameOver()) send(client, roomId, 'draw-decline');
  };

  const startGame = (client: Client, roomId: number) => {
    clickSound.play();
    send(client, roomId, 'start');
  };

  return {
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
  };
}
