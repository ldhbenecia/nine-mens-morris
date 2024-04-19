import { StoneType } from '~/lib/types';

type PointProps = {
  top: number;
  left: number;
  stone: StoneType;
  selected: boolean;
};

export function Point({ stone = 'EMPTY', top, left, selected }: PointProps) {
  return (
    <div
      className="absolute flex h-[80px] w-[80px] cursor-pointer items-center justify-center"
      style={{ transform: `translate(${-40 + left}px, ${-40 + top}px)` }}
    >
      {stone === 'EMPTY' ? (
        <div
          className={`flex h-[60px] w-[60px] items-center justify-center rounded-full transition-colors hover:bg-[rgba(107,114,128,0.1)]`}
        >
          <div className="h-[8px] w-[8px] rounded-full bg-gray-400 " />
        </div>
      ) : (
        <div
          className={`flex h-[60px] w-[60px] animate-stone-set items-center justify-center rounded-full`}
        >
          <div className="pointer-events-none absolute z-20 h-[60px] w-[60px] animate-stone-set-effect rounded-full border border-slate-400" />
          <div
            className={`absolute h-full w-full rounded-full bg-red-600 ${selected ? 'opacity-50' : 'opacity-0'}`}
          />
          <div
            className={`h-full w-full rounded-full shadow-stone ${stone === 'WHITE' ? 'bg-white-stone' : 'bg-black-stone'}`}
          />
          <div
            className={`absolute -z-10 h-[115%] w-[115%] animate-stone-selected-effect rounded-full bg-selected-stone ${selected ? '' : 'opacity-0'}`}
          />
        </div>
      )}
    </div>
  );
}
