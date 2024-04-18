import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { QUERY } from '~/lib/queries';
import { Client } from '@stomp/stompjs';
import { GameState } from '~/lib/types';
import { NEIGHBOR, TRIPLE } from '~/lib/constants';

const initialGameState: GameState = {
  board: [...Array(24)].map(() => 'EMPTY'),
  hostId: -1,
  guestId: -1,
  currentTurn: null,
  hostAddable: 9,
  guestAddable: 9,
  hostTotal: 9,
  guestTotal: 9,
  status: 'WAITING',
  phase: 1,
  removing: false,
  winner: null,
  loser: null,
};

export function useGameState() {
  const [gameState, setGameState] = useState<GameState>(initialGameState);
  const { data: currentUser } = useQuery(QUERY.CURRENT_USER);

  const isRemovingStage = () => {
    return gameState.removing;
  };

  const isGameOver = () => {
    return gameState.status === 'FINISHED';
  };

  const isPlayerTurn = () => {
    return gameState.currentTurn === currentUser?.userId;
  };

  const isPlayerHost = () => {
    return gameState.hostId === currentUser?.userId;
  };

  const getPlayerStoneColor = () => {
    return isPlayerHost() ? 'BLACK' : 'WHITE';
  };

  const getEnemyStoneColor = () => {
    return isPlayerHost() ? 'WHITE' : 'BLACK';
  };

  const getPlayerAddable = () => {
    return isPlayerHost() ? gameState.hostAddable : gameState.guestAddable;
  };

  const getEnemyAddable = () => {
    return !isPlayerHost() ? gameState.hostAddable : gameState.guestAddable;
  };

  const getPlayerTotal = () => {
    return isPlayerHost() ? gameState.hostTotal : gameState.guestTotal;
  };

  const getEnemyTotal = () => {
    return !isPlayerHost() ? gameState.hostTotal : gameState.guestTotal;
  };

  const getCurrentPhase = () => {
    return gameState.phase;
  };

  const isEmptyPoint = (index: number) => {
    return gameState.board[index] === 'EMPTY';
  };

  const isPlayerPoint = (index: number) => {
    return gameState.board[index] === getPlayerStoneColor();
  };

  const isEnemyPoint = (index: number) => {
    return gameState.board[index] === getEnemyStoneColor();
  };

  const isAdjacent = (from: number, to: number) => {
    return NEIGHBOR[from].includes(to);
  };

  // 제거하려는 돌이 이미 3연속 배열을 이루고 있는가?
  const isBelongToTriple = (index: number) => {
    return TRIPLE.some(
      (positions) =>
        positions.includes(index) &&
        positions.every(
          (position) => gameState.board[position] === getEnemyStoneColor()
        )
    );
  };

  const addStone = (client: Client, roomId: number, index: number) => {
    if (
      !isGameOver() &&
      getCurrentPhase() === 1 &&
      !isRemovingStage() &&
      isPlayerTurn() &&
      isEmptyPoint(index) &&
      (isPlayerHost() ? gameState.hostAddable : gameState.guestAddable) > 0
    ) {
      client.publish({
        destination: `/app/game/placeStone`,
        body: JSON.stringify({
          gameId: roomId,
          initialPosition: index,
          finalPosition: 99,
        }),
      });
      client.publish({
        destination: `/topic/game/${roomId}`,
        body: JSON.stringify({
          type: 'CLIENT_EVENT',
          contents: `ADD_${getPlayerStoneColor()}`,
        }),
      });
    }
  };

  const moveStone = (
    client: Client,
    roomId: number,
    from: number,
    to: number
  ) => {
    if (
      !isGameOver() &&
      getCurrentPhase() === 2 &&
      !isRemovingStage() &&
      isPlayerTurn() &&
      isPlayerPoint(from) &&
      isEmptyPoint(to) &&
      isAdjacent(from, to)
    ) {
      client.publish({
        destination: `/app/game/placeStone`,
        body: JSON.stringify({
          gameId: roomId,
          initialPosition: from,
          finalPosition: to,
        }),
      });
      client.publish({
        destination: `/topic/game/${roomId}`,
        body: JSON.stringify({
          type: 'CLIENT_EVENT',
          contents: `ADD_${getPlayerStoneColor()}`,
        }),
      });
    }
  };

  const removeStone = (client: Client, roomId: number, index: number) => {
    if (
      !isGameOver() &&
      isRemovingStage() &&
      isPlayerTurn() &&
      isEnemyPoint(index) &&
      !isBelongToTriple(index)
    ) {
      client.publish({
        destination: `/app/game/removeOpponentStone`,
        body: JSON.stringify({
          gameId: roomId,
          removePosition: index,
        }),
      });
      client.publish({
        destination: `/topic/game/${roomId}`,
        body: JSON.stringify({
          type: 'CLIENT_EVENT',
          contents: 'REMOVE_STONE',
        }),
      });
    }
  };

  const skipRemoving = (client: Client, roomId: number) => {
    if (!isGameOver() && isRemovingStage() && isPlayerTurn()) {
      client.publish({
        destination: `/app/game/removeOpponentStone`,
        body: JSON.stringify({
          gameId: roomId,
          removePosition: 99,
        }),
      });
    }
  };

  const withdraw = (client: Client, roomId: number) => {
    if (!isGameOver() && currentUser) {
      client.publish({
        destination: `/app/game/withdraw`,
        body: JSON.stringify({
          gameId: roomId,
          userId: currentUser.userId,
        }),
      });
    }
  };

  return {
    gameState,
    setGameState,
    isPlayerHost,
    isPlayerTurn,
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
  };
}
