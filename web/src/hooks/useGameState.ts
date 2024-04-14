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
  isRemoving: false,
  winner: null,
  loser: null,
};

export function useGameState() {
  const [gameState, setGameState] = useState<GameState>(initialGameState);
  const { data: currentUser } = useQuery(QUERY.CURRENT_USER);

  const isRemovingStage = () => {
    return gameState.isRemoving;
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
      getCurrentPhase() === 1 &&
      !isRemovingStage() &&
      isPlayerTurn() &&
      isEmptyPoint(index) &&
      (isPlayerHost() ? gameState.hostAddable : gameState.guestAddable) > 0
    ) {
      console.log(
        JSON.stringify({
          gameId: roomId,
          initialPosition: index,
          finalPosition: 99,
        })
      );
      client.publish({
        destination: `/app/game/placeStone`,
        body: JSON.stringify({
          gameId: roomId,
          initialPosition: index,
          finalPosition: 99,
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
    }
  };

  const removeStone = (client: Client, roomId: number, index: number) => {
    if (
      isRemovingStage() &&
      isPlayerTurn() &&
      isEnemyPoint(index) &&
      !isBelongToTriple(index)
    ) {
      const newBoard = [...gameState.board];
      newBoard[index] = 'EMPTY';
      client.publish({
        destination: `/app/game/removeOpponentStone`,
        body: JSON.stringify({
          gameId: roomId,
          removePosition: index,
        }),
      });
    }
  };

  return {
    gameState,
    setGameState,
    isPlayerHost,
    isPlayerTurn,
    getPlayerStoneColor,
    getEnemyStoneColor,
    getPlayerAddable,
    getEnemyAddable,
    getPlayerTotal,
    getEnemyTotal,
    addStone,
    moveStone,
    removeStone,
  };
}
