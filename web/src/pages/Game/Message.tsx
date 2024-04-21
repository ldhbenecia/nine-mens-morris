type MessageProps = {
  phase: number;
  turn: boolean;
  error: string;
  removing: boolean;
  flying: boolean;
  onSkipRemoving: () => void;
};

export function Message({
  phase,
  turn,
  error,
  removing,
  flying,
  onSkipRemoving,
}: MessageProps) {
  return (
    <div className="z-10 flex flex-col items-center py-2">
      {error && <span className="animate-shaking text-red-600">{error}</span>}
      <div
        className={`flex items-center gap-1 ${turn ? 'visible' : 'invisible'}  ${removing ? 'text-red-800' : ''}`}
      >
        <span className="animate-pulse">
          {removing ? (
            '상대의 돌 중 하나를 선택해 제거하세요.'
          ) : phase === 1 ? (
            '빈 지점에 돌을 배치하세요.'
          ) : flying ? (
            <b>돌 3개 남음: 모든 지점으로 돌을 옮기세요!</b>
          ) : (
            '돌을 인접한 지점으로 옮길 수 있습니다.'
          )}
        </span>
        {removing && (
          <div
            onClick={onSkipRemoving}
            className="cursor-pointer p-1 font-semibold hover:text-red-700 active:text-red-900"
          >
            [SKIP]
          </div>
        )}
      </div>
    </div>
  );
}
