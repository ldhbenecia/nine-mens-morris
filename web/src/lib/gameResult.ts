import { GameState } from '~/lib/types';

export type GameResult = 'WIN' | 'LOSS' | 'DRAW';

// 서버는 흑/백 기준으로 승자를 주므로 내 id 와 맞춰 봐야 승패가 정해짐
export function resultOf(state: GameState, myUserId: number): GameResult {
  if (state.winnerId === myUserId) return 'WIN';
  if (state.loserId === myUserId) return 'LOSS';
  return 'DRAW';
}
