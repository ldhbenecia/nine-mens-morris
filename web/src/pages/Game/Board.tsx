import { useMemo, useState } from 'react';
import { PointType, StoneType } from '~/lib/types';
import { MILLS, STONE_POSITION } from '~/lib/constants';
import { Point } from './Point';

// 직전 수에서 일어난 일. 화면 연출에만 씀
export type StoneMotion =
  | { kind: 'MOVE'; from: number; to: number }
  | { kind: 'REMOVE'; point: number; stone: StoneType };

type BoardProps = {
  board: StoneType[];
  playerStoneColor: StoneType;
  selectable: boolean;
  placeable: boolean; // 내 차례의 배치 단계. 빈 지점에 내 돌을 미리 보여줌
  removableStone: StoneType; // 3연속 뒤 제거할 수 있는 색. 보호 여부는 서버가 판단
  lastPoint: number | null;
  motion: StoneMotion | null;
  millFormed: boolean;
  onPlace: (point: number) => void;
  onMove: (from: number, to: number) => void;
  onRemove: (point: number) => void;
  onReject: (message: string) => void;
};

export function Board({
  board,
  playerStoneColor,
  selectable,
  placeable,
  removableStone,
  lastPoint,
  motion,
  millFormed,
  onPlace,
  onMove,
  onRemove,
  onReject,
}: BoardProps) {
  const [selectedStone, setSelectedStone] = useState<number | null>(null);

  // board 에서 계산되는 값이라 state 로 복사하지 않음
  // 복사하면 착수마다 렌더가 한 번 더 일어나고 그 사이 한 프레임은 이전 판이 보임
  const points = useMemo<PointType[]>(
    () => board.map((stone, index) => ({ ...STONE_POSITION[index], stone })),
    [board]
  );

  // 마지막 돌이 낀 줄 중 같은 색으로 찬 줄. 두 줄이 동시에 생기면 둘 다
  const millPoints = useMemo(() => {
    if (!millFormed || lastPoint === null) return new Set<number>();
    const color = board[lastPoint];
    return new Set(
      MILLS.filter(
        (mill) =>
          mill.includes(lastPoint) && mill.every((p) => board[p] === color)
      ).flat()
    );
  }, [board, lastPoint, millFormed]);

  const slideFromOf = (idx: number) => {
    if (motion?.kind !== 'MOVE' || motion.to !== idx) return null;
    const from = STONE_POSITION[motion.from];
    const to = STONE_POSITION[motion.to];
    return { x: from.left - to.left, y: from.top - to.top };
  };

  const preview =
    removableStone === 'EMPTY' && (placeable || selectedStone !== null)
      ? playerStoneColor
      : 'EMPTY';

  // 판정은 서버가 함. 여기선 눌러 볼 만한 자리인지만 봄
  const clickableOf = (stone: StoneType) => {
    if (removableStone !== 'EMPTY') return stone === removableStone;
    if (stone === 'EMPTY') return placeable || selectedStone !== null;
    return stone === playerStoneColor && selectable;
  };

  const onClickPoint = (idx: number) => {
    switch (points[idx].stone) {
      case 'EMPTY':
        if (selectedStone !== null) {
          onMove(selectedStone, idx);
          setSelectedStone(null);
          break;
        }
        onPlace(idx);
        break;
      default:
        // 제거 차례면 어느 돌이든 서버로. 보호 여부·자기 돌 여부는 서버가 답함
        if (removableStone !== 'EMPTY') {
          onRemove(idx);
        } else if (points[idx].stone === playerStoneColor && selectable) {
          setSelectedStone(selectedStone === idx ? null : idx);
        } else if (placeable) {
          // 서버가 "이미 돌이 놓인 지점" 으로 답함
          onPlace(idx);
        } else if (selectable) {
          onReject('자기 돌만 옮길 수 있습니다.');
        } else {
          // 이동 단계의 돌 고르기는 화면에서만 일어나 서버가 답할 기회가 없음
          // 그냥 두면 상대 차례에 내 돌을 눌러도 아무 반응이 없음
          onReject('상대 차례입니다.');
        }
        break;
    }
  };

  return (
    // scale 은 레이아웃 크기를 줄이지 않음. 바깥 상자를 줄어든 크기로 잡아야 위아래 글자와 겹치지 않음
    // 여백은 가장자리 지점의 돌이 판 밖으로 반지름만큼 나가는 몫
    <div className="m-6 h-[240px] w-[240px] shrink-0 xs:h-[360px] xs:w-[360px] md:h-[432px] md:w-[432px] lg:h-[480px] lg:w-[480px]">
      <div className="h-[480px] w-[480px] origin-top-left scale-50 xs:scale-75 md:scale-90 lg:scale-100">
        <div className="absolute z-10">
          {points.map((point, idx) => (
            <Point
              key={idx}
              point={idx}
              top={point.top}
              left={point.left}
              stone={point.stone}
              selected={idx === selectedStone}
              preview={preview}
              removable={
                removableStone !== 'EMPTY' && point.stone === removableStone
              }
              last={idx === lastPoint}
              inMill={millPoints.has(idx)}
              slideFrom={slideFromOf(idx)}
              removedStone={
                motion?.kind === 'REMOVE' && motion.point === idx
                  ? motion.stone
                  : 'EMPTY'
              }
              clickable={clickableOf(point.stone)}
              onClick={() => onClickPoint(idx)}
            />
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
