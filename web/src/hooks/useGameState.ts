import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { QUERY } from '~/lib/queries';
import { Client } from '@stomp/stompjs';
import { GameState } from '~/lib/types';

export function useGameState(initialGameState: GameState) {
  const [gameState, setGameState] = useState(initialGameState);
  const { data: currentUser } = useQuery(QUERY.CURRENT_USER);

  const updateGameState = (newState: GameState) => {
    setGameState(newState);
  };

  const isPlayerTurn = () => {
    return gameState.currentTurn === currentUser?.userId;
  };

  const getPlayerIndex = () => {
    return gameState.players.indexOf(currentUser?.userId || -1);
  };

  const getPlayerStoneColor = () => {
    return getPlayerIndex() === 0 ? 'BLACK' : 'WHITE';
  };

  const getCurrentPhase = () => {
    return gameState.phase;
  };

  const countAddableStone = () => {
    return gameState.addable[getPlayerIndex()];
  };

  const countTotalStone = () => {
    return gameState.total[getPlayerIndex()];
  };

  const isEmptyPoint = (index: number) => {
    return gameState.board[index] === 'EMPTY';
  };

  const isPlayerPoint = (index: number) => {
    return gameState.board[index] === getPlayerStoneColor();
  };

  const isEnemyPoint = (index: number) => {
    return (
      gameState.board[index] !== 'EMPTY' &&
      gameState.board[index] !== getPlayerStoneColor()
    );
  };

  const addStone = (client: Client, index: number) => {
    if (
      getCurrentPhase() === 1 &&
      isPlayerTurn() &&
      isEmptyPoint(index) &&
      countAddableStone() > 0
    ) {
      client.publish({
        destination: `/app/game/placeStone`,
        body: JSON.stringify({
          gameId: gameState.roomId,
          initialPosition: index,
          finalPosition: 99,
        }),
      });
    }
  };

  const moveStone = (client: Client, from: number, to: number) => {
    if (
      getCurrentPhase() === 2 &&
      isPlayerTurn() &&
      isPlayerPoint(from) &&
      isEmptyPoint(to)
    ) {
      client.publish({
        destination: `/app/game/placeStone`,
        body: JSON.stringify({
          gameId: gameState.roomId,
          initialPosition: from,
          finalPosition: to,
        }),
      });
    }
  };

  const removeStone = (client: Client, index: number) => {
    if (isPlayerTurn() && isEnemyPoint(index)) {
      client.publish({
        destination: `/app/game/placeStone`,
        body: JSON.stringify({
          gameId: gameState.roomId,
          removePosition: index,
        }),
      });
    }
  };

  return {
    gameState,
    updateGameState,
    isPlayerTurn,
    getPlayerStoneColor,
    countAddableStone,
    countTotalStone,
    addStone,
    moveStone,
    removeStone,
  };
}
