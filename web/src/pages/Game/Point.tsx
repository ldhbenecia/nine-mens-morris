import { StoneType } from '~/lib/types';

type PointProps = {
  point: number;
  top: number;
  left: number;
  stone: StoneType;
  selected: boolean;
  preview: StoneType; // 빈 지점에 올렸을 때 반투명하게 보일 돌. EMPTY 면 안 보임
  removable: boolean;
  last: boolean; // 방금 둔 자리. 상대가 어디 뒀는지 놓치지 않게
  inMill: boolean; // 방금 3연속이 된 줄
  slideFrom: { x: number; y: number } | null; // 이동해 온 돌이면 출발 지점까지의 거리
  removedStone: StoneType; // 방금 제거된 돌. 사라지는 모습만 그림
  onClick: () => void;
};

const STONE_BACKGROUND = {
  WHITE: 'bg-white-stone',
  BLACK: 'bg-black-stone',
  EMPTY: '',
};

export function Point({
  point,
  stone,
  top,
  left,
  selected,
  preview,
  removable,
  last,
  inMill,
  slideFrom,
  removedStone,
  onClick,
}: PointProps) {
  return (
    <button
      type="button"
      aria-label={`지점 ${point}`}
      onClick={onClick}
      // 지나가는 길의 다른 돌 위로 그려지게. transform 때문에 z 는 버튼 단위로만 먹힘
      className={`group absolute flex h-[80px] w-[80px] cursor-pointer items-center justify-center ${slideFrom ? 'z-30' : ''}`}
      style={{ transform: `translate(${-40 + left}px, ${-40 + top}px)` }}
    >
      {stone === 'EMPTY' ? (
        <div className="flex h-[60px] w-[60px] items-center justify-center rounded-full transition-colors group-hover:bg-[rgba(107,114,128,0.1)]">
          {preview !== 'EMPTY' && (
            <div
              className={`absolute h-[60px] w-[60px] rounded-full opacity-0 transition-opacity group-hover:opacity-40 ${STONE_BACKGROUND[preview]}`}
            />
          )}
          {removedStone !== 'EMPTY' && (
            <div
              className={`pointer-events-none absolute h-[60px] w-[60px] animate-stone-remove rounded-full shadow-stone ${STONE_BACKGROUND[removedStone]}`}
            />
          )}
          <div className="h-[8px] w-[8px] rounded-full bg-gray-400" />
        </div>
      ) : (
        // 이동해 온 돌은 착수 파동 대신 출발 지점에서 미끄러져 옴
        <div
          className={`flex h-[60px] w-[60px] items-center justify-center rounded-full ${slideFrom ? 'animate-stone-slide' : 'animate-stone-set'} ${inMill ? 'animate-mill-glow' : ''}`}
          style={
            slideFrom
              ? ({
                  '--slide-x': `${slideFrom.x}px`,
                  '--slide-y': `${slideFrom.y}px`,
                } as React.CSSProperties)
              : undefined
          }
        >
          {!slideFrom && (
            <div className="pointer-events-none absolute z-20 h-[60px] w-[60px] animate-stone-set-effect rounded-full border border-slate-400" />
          )}
          <div
            className={`absolute h-full w-full rounded-full bg-red-600 ${selected ? 'opacity-50' : 'opacity-0'}`}
          />
          <div
            className={`h-full w-full rounded-full shadow-stone ${STONE_BACKGROUND[stone]}`}
          />
          {removable && (
            <div className="pointer-events-none absolute -inset-1.5 animate-pulse rounded-full border-4 border-red-500" />
          )}
          {last && (
            <div
              className={`pointer-events-none absolute h-2.5 w-2.5 rounded-full ${stone === 'WHITE' ? 'bg-gray-500' : 'bg-gray-300'}`}
            />
          )}
          <div
            className={`absolute -z-10 h-[115%] w-[115%] animate-stone-selected-effect rounded-full bg-selected-stone ${selected ? '' : 'opacity-0'}`}
          />
        </div>
      )}
    </button>
  );
}
