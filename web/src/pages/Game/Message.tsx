import { Phase } from '~/lib/types';

type MessageProps = {
  phase: Phase;
  turn: boolean;
  error: string;
  removing: boolean;
};

// 제거를 건너뛰는 버튼은 없음. 상대 돌이 전부 3연속이면 그 돌도 제거할 수 있어 고를 돌이 없는 상황이 없음
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
