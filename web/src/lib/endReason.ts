import { EndReason } from '~/lib/types';

// 서버가 보내는 종료 사유의 화면 문구
// Record 라서 서버에 새 사유가 생겼는데 여기를 빠뜨리면 타입 검사에서 걸림
// 승자·패자 양쪽 화면에 같은 문구가 뜨므로 주어 없이 씀
export const END_REASON_LABEL: Record<EndReason, string> = {
  STONES_EXHAUSTED: '돌이 2개만 남아 승부가 났습니다',
  NO_LEGAL_MOVE: '움직일 수 있는 돌이 없어 승부가 났습니다',
  RESIGN: '기권으로 승부가 났습니다',
  DRAW_AGREED: '서로 무승부에 합의했습니다',
  THREEFOLD_REPETITION: '같은 국면이 3번 반복되어 무승부입니다',
  FIFTY_MOVE_RULE: '50수 동안 돌이 제거되지 않아 무승부입니다',
  ABANDONED: '양쪽 모두 오래 두지 않아 판을 접었습니다',
};
