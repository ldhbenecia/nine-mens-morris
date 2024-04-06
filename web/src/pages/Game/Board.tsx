import { useState } from 'react';
import { Point } from './Point';
import { PointType } from '~/types';

export function Board() {
  const [points, setPoints] = useState<PointType[]>([
    { top: 0, left: 0, stone: 'EMPTY', selected: false },
    { top: 0, left: 240, stone: 'EMPTY', selected: false },
    { top: 0, left: 480, stone: 'EMPTY', selected: false },

    { top: 80, left: 80, stone: 'EMPTY', selected: false },
    { top: 80, left: 240, stone: 'EMPTY', selected: false },
    { top: 80, left: 400, stone: 'EMPTY', selected: false },

    { top: 160, left: 160, stone: 'EMPTY', selected: false },
    { top: 160, left: 240, stone: 'EMPTY', selected: false },
    { top: 160, left: 320, stone: 'EMPTY', selected: false },

    { top: 240, left: 0, stone: 'EMPTY', selected: false },
    { top: 240, left: 80, stone: 'EMPTY', selected: false },
    { top: 240, left: 160, stone: 'EMPTY', selected: false },
    { top: 240, left: 320, stone: 'EMPTY', selected: false },
    { top: 240, left: 400, stone: 'EMPTY', selected: false },
    { top: 240, left: 480, stone: 'EMPTY', selected: false },

    { top: 320, left: 160, stone: 'EMPTY', selected: false },
    { top: 320, left: 240, stone: 'EMPTY', selected: false },
    { top: 320, left: 320, stone: 'EMPTY', selected: false },

    { top: 400, left: 80, stone: 'EMPTY', selected: false },
    { top: 400, left: 240, stone: 'EMPTY', selected: false },
    { top: 400, left: 400, stone: 'EMPTY', selected: false },

    { top: 480, left: 0, stone: 'EMPTY', selected: false },
    { top: 480, left: 240, stone: 'EMPTY', selected: false },
    { top: 480, left: 480, stone: 'EMPTY', selected: false },
  ]);

  return (
    <div>
      <div className="absolute z-10 h-[480px] w-[480px]">
        {points.map(({ top, left, stone, selected }, idx) => (
          <Point
            key={idx}
            top={top}
            left={left}
            stone={stone}
            selected={selected}
          />
        ))}
      </div>
      <div className="relative h-[480px] w-[480px]">
        <div className="flex h-[480px] w-[480px] items-center justify-center border border-gray-400">
          <div className="flex h-[320px] w-[320px] items-center justify-center border border-gray-400">
            <div className="h-[160px] w-[160px] border border-gray-400" />
          </div>
        </div>
        <div className="absolute left-[240px] top-0 h-[160px] border-l border-gray-400" />
        <div className="absolute bottom-0 left-[240px] h-[160px] border-l border-gray-400" />
        <div className="absolute left-0 top-[240px] w-[160px] border-t border-gray-400" />
        <div className="absolute right-0 top-[240px] w-[160px] border-t border-gray-400" />
      </div>
    </div>
  );
}
