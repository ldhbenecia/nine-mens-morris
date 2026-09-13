import { Phase } from '~/lib/types';

type MessageProps = {
  phase: Phase;
  turn: boolean;
  error: string;
  removing: boolean;
};

// SKIP 버튼이 사라졌다
// 예전에는 제거 대상이 전부 3연속이라 고를 수 없을 때 빠져나갈 방법이 SKIP 뿐이었는데,
// 서버가 "상대 돌이 전부 3연속이면 그마저 제거 가능" 예외를 구현해서
// 이제 고를 수 없는 상황 자체가 없다
export function Message({ phase, turn, error, removing }: MessageProps) {
  const guide = () => {
    if (removing) return '상대 돌 하나를 골라 제거하세요';
    if (phase === 'PLACING') return '빈 지점에 돌을 놓으세요';
    if (phase === 'FLYING') return '돌이 3개! 이제 어디로든 옮길 수 있습니다';
    return '인접한 빈 지점으로 돌을 옮기세요';
  };

  return (
    <div className="z-10 flex flex-col items-center py-2">
      {error && <span className="animate-shaking text-red-600">{error}</span>}
      <div
        className={`flex items-center gap-1 ${turn ? 'visible' : 'invisible'} ${removing ? 'text-red-800' : ''}`}
      >
        <span className="animate-pulse">
          {phase === 'FLYING' && !removing ? <b>{guide()}</b> : guide()}
        </span>
      </div>
    </div>
  );
}
