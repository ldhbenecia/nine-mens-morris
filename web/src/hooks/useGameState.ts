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
};

let errorTimerId = 0;

/**
 * 규칙 판단은 하지 않는다.
 *
 * 예전에는 턴 / 인접 / 3연속 / 빈 칸을 여기서 검사한 뒤에야 서버로 보냈다.
 * 서버에는 같은 검사가 아예 없어서 규칙 강제가 사실상 프론트에만 있었고,
 * 규칙 표(인접·3연속)가 양쪽에 복제되어 한쪽만 고치면 어긋났다.
 *
 * 이제 서버가 판정하고 거절 사유를 개인 큐로 보내준다.
 * 여기서는 "화면상 명백히 누를 수 없는 것"만 막고 나머지는 서버에 맡긴다.
 */
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

  const isEmptyPoint = (index: number) => gameState.board[index] === 'EMPTY';

  const isPlayerPoint = (index: number) => gameState.board[index] === myStone();

  const isEnemyPoint = (index: number) =>
    gameState.board[index] === enemyStone();

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

  // 손에 든 돌이 없는데 놓으려 하거나 이미 찬 자리를 누른 것은
  // 서버에 물어볼 것도 없이 화면에서 막는다
  const addStone = (client: Client, roomId: number, index: number) => {
    if (isGameOver() || gameState.awaitingRemoval || !isPlayerTurn()) return;
    if (getPlayerPhase() !== 'PLACING' || !isEmptyPoint(index)) return;

    send(client, roomId, 'place', { to: index });
  };

  // 인접 여부는 검사하지 않는다. FLYING 인지까지 따지면 규칙이 다시 복제된다
  const moveStone = (
    client: Client,
    roomId: number,
    from: number,
    to: number
  ) => {
    if (isGameOver() || gameState.awaitingRemoval || !isPlayerTurn()) return;
    if (getPlayerPhase() === 'PLACING') return;
    if (!isPlayerPoint(from) || !isEmptyPoint(to)) return;

    send(client, roomId, 'move', { from, to });
  };

  // 3연속에 속한 돌인지도 서버가 판단한다.
  // 상대 돌이 전부 3연속이면 그마저 제거할 수 있다는 예외가 있어
  // 프론트에서 판단하면 틀린다
  const removeStone = (client: Client, roomId: number, index: number) => {
    if (isGameOver() || !gameState.awaitingRemoval || !isPlayerTurn()) return;
    if (!isEnemyPoint(index)) return;

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

  // 새로고침 후 판을 되찾는다
  const requestSync = (client: Client, roomId: number) => {
    send(client, roomId, 'sync');
  };

  return {
    gameState,
    error,
    setGameState,
    showError,
    isBlack,
    isPlayerTurn,
    isGameOver,
    myStone,
    enemyStone,
    enemyId,
    getPlayerInHand,
    getEnemyInHand,
    getPlayerOnBoard,
    getEnemyOnBoard,
    getPlayerPhase,
    isEmptyPoint,
    isPlayerPoint,
    isEnemyPoint,
    addStone,
    moveStone,
    removeStone,
    resign,
    offerDraw,
    acceptDraw,
    declineDraw,
    startGame,
    requestSync,
  };
}
