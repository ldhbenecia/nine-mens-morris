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
  onClick,
}: PointProps) {
  return (
    <button
      type="button"
      aria-label={`지점 ${point}`}
      onClick={onClick}
      className="group absolute flex h-[80px] w-[80px] cursor-pointer items-center justify-center"
      style={{ transform: `translate(${-40 + left}px, ${-40 + top}px)` }}
    >
      {stone === 'EMPTY' ? (
        <div className="flex h-[60px] w-[60px] items-center justify-center rounded-full transition-colors group-hover:bg-[rgba(107,114,128,0.1)]">
          {preview !== 'EMPTY' && (
            <div
              className={`absolute h-[60px] w-[60px] rounded-full opacity-0 transition-opacity group-hover:opacity-40 ${STONE_BACKGROUND[preview]}`}
            />
          )}
          <div className="h-[8px] w-[8px] rounded-full bg-gray-400" />
        </div>
      ) : (
        <div className="flex h-[60px] w-[60px] animate-stone-set items-center justify-center rounded-full">
          <div className="pointer-events-none absolute z-20 h-[60px] w-[60px] animate-stone-set-effect rounded-full border border-slate-400" />
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
            <div className="pointer-events-none absolute h-2.5 w-2.5 rounded-full bg-amber-400 shadow-sm" />
          )}
          <div
            className={`absolute -z-10 h-[115%] w-[115%] animate-stone-selected-effect rounded-full bg-selected-stone ${selected ? '' : 'opacity-0'}`}
          />
        </div>
      )}
    </button>
  );
}
