type MessageProps = {
  phase: number;
  turn: boolean;
  removing: boolean;
  onSkipRemoving: () => void;
};

export function Message({
  phase,
  turn,
  removing,
  onSkipRemoving,
}: MessageProps) {
  return (
    <div
      className={`flex items-center gap-1 py-2 ${turn ? 'visible' : 'invisible'}  ${removing ? 'text-red-800' : ''}`}
    >
      <span className="animate-pulse">
        {removing
          ? '상대의 돌 중 하나를 선택해 제거하세요.'
          : phase === 1
            ? '빈 지점에 돌을 배치하세요.'
            : '돌을 인접한 지점으로 옮길 수 있습니다.'}
      </span>
      {removing && (
        <div
          onClick={onSkipRemoving}
          className="cursor-pointer p-1 hover:text-red-700 active:text-red-900"
        >
          [SKIP]
        </div>
      )}
    </div>
  );
}
