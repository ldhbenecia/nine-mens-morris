import { PointType } from '~/types';

export function Point({
  stone = 'EMPTY',
  top,
  left,
  selected = false,
}: PointType) {
  return (
    <div
      className="absolute flex h-[80px] w-[80px] items-center justify-center"
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
          className={`animate-stone-set flex h-[60px] w-[60px] items-center justify-center rounded-full ${selected ? 'border-2 border-dashed border-red-600' : ''}`}
        >
          <div className="animate-stone-set-effect pointer-events-none absolute z-20 h-[60px] w-[60px] rounded-full border border-gray-400" />
          <div
            className={`shadow-stone h-full w-full rounded-full ${stone === 'WHITE' ? 'bg-white-stone' : 'bg-black-stone'} ${selected ? 'opacity-50' : ''}`}
          />
        </div>
      )}
    </div>
  );
}
