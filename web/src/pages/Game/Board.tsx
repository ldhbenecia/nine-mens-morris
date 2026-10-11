import { useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import { PointType, StoneType } from '~/lib/types';
import { STONE_POSITION } from '~/lib/constants';
import { Point } from './Point';

type BoardProps = {
  client: Client;
  board: StoneType[];
  playerStoneColor: StoneType;
  selectable: boolean;
  addStone: (client: Client, roomId: number, index: number) => void;
  moveStone: (client: Client, roomId: number, from: number, to: number) => void;
  removeStone: (client: Client, roomId: number, index: number) => void;
};

export function Board({
  client,
  board,
  playerStoneColor,
  selectable,
  addStone,
  moveStone,
  removeStone,
}: BoardProps) {
  const { roomId } = useParams();
  const [selectedStone, setSelectedStone] = useState<number | null>(null);

  // board 에서 계산되는 값이라 state 로 복사하지 않음
  // 복사하면 착수마다 렌더가 한 번 더 일어나고 그 사이 한 프레임은 이전 판이 보임
  const points = useMemo<PointType[]>(
    () => board.map((stone, index) => ({ ...STONE_POSITION[index], stone })),
    [board]
  );

  const onClickPoint = (idx: number) => {
    if (!roomId) return;

    switch (points[idx].stone) {
      case 'EMPTY':
        if (selectedStone !== null) {
          moveStone(client, Number(roomId), selectedStone, idx);
          setSelectedStone(null);
          break;
        }
        addStone(client, Number(roomId), idx);
        break;
      case playerStoneColor:
        if (selectable) {
          setSelectedStone(selectedStone === idx ? null : idx);
        }
        break;
      default:
        removeStone(client, Number(roomId), idx);
        break;
    }
  };

  return (
    <div className="absolute top-0 left-0 flex h-full w-full items-center justify-center overflow-hidden">
      <div className="h-[480px] w-[480px] scale-50 xs:scale-75 md:scale-90 lg:scale-100">
        <div className="absolute z-10">
          {points.map((point, idx) => (
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
        <div className="flex h-[480px] w-[480px] items-center justify-center border border-gray-400">
          <div className="flex h-[320px] w-[320px] items-center justify-center border border-gray-400">
            <div className="h-[160px] w-[160px] border border-gray-400" />
          </div>
        </div>
        <div className="absolute top-0 left-[240px] h-[160px] border-l border-gray-400" />
        <div className="absolute bottom-0 left-[240px] h-[160px] border-l border-gray-400" />
        <div className="absolute top-[240px] left-0 w-[160px] border-t border-gray-400" />
        <div className="absolute top-[240px] right-0 w-[160px] border-t border-gray-400" />
      </div>
    </div>
  );
}
