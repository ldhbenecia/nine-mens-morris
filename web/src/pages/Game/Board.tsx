import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import { PointType, StoneType } from '~/lib/types';
import { STONE_POSITION } from '~/lib/constants';
import { Point } from './Point';

type BoardProps = {
  client: Client;
  board: StoneType[];
  playerStoneColor: StoneType;
  addStone: (client: Client, roomId: number, index: number) => void;
  moveStone: (client: Client, roomId: number, from: number, to: number) => void;
  removeStone: (client: Client, roomId: number, index: number) => void;
};

export function Board({
  client,
  board,
  playerStoneColor,
  addStone,
  moveStone,
  removeStone,
}: BoardProps) {
  const { roomId } = useParams();
  const [points, setPoints] = useState<PointType[] | null>(null);
  const [selectedStone, setSelectedStone] = useState<number | null>(null);

  const onClickPoint = (idx: number) => {
    if (!points || !roomId) return;

    switch (points[idx].stone) {
      case 'EMPTY':
        if (selectedStone !== null) {
          moveStone(client, Number(roomId), selectedStone, idx);
          break;
        }

        addStone(client, Number(roomId), idx);
        break;
      case playerStoneColor:
        setSelectedStone(selectedStone === idx ? null : idx);
        break;
      default:
        removeStone(client, Number(roomId), idx);
        break;
    }
  };

  useEffect(() => {
    setPoints(
      board.map((stone, index) => ({
        ...STONE_POSITION[index],
        stone,
      }))
    );
  }, [board]);

  return (
    <div className="-m-24 flex h-[480px] w-[480px] shrink grow scale-50 flex-col items-center justify-center gap-8 self-center xs:-m-6 xs:scale-75 md:-m-0 md:scale-90 lg:scale-100">
      <div className="absolute z-10 h-[480px] w-[480px]">
        {points?.map((point, idx) => (
          <div key={idx} onClick={() => onClickPoint(idx)}>
            <Point
              top={point.top}
              left={point.left}
              stone={point.stone}
              selected={idx === selectedStone}
            />
          </div>
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
