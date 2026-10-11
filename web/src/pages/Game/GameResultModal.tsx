import { Button, Modal } from '~/components';
import { useCountUp } from '~/hooks';
import { END_REASON_LABEL } from '~/lib/endReason';
import { GameResult } from '~/lib/gameResult';
import { EndReason } from '~/lib/types';

export type RatingChange = { before: number; after: number };

type GameResultModalProps = {
  visible: boolean;
  result: GameResult;
  endReason: EndReason | null;
  notice?: string; // 상대 연결 끊김처럼 종료 사유 말고 더 알려줄 것이 있을 때
  rating: RatingChange | 'PENDING' | null; // 랭크전이 아니면 null. 반영된 점수를 다시 받는 동안 PENDING
  onLeaveRoom: () => void;
};

const RESULT_TITLE: Record<GameResult, string> = {
  WIN: '승리했습니다!',
  LOSS: '패배했습니다...',
  DRAW: '무승부!',
};

export function GameResultModal({
  visible,
  result,
  endReason,
  notice,
  rating,
  onLeaveRoom,
}: GameResultModalProps) {
  return (
    <Modal visible={visible}>
      <>
        <div className="text-2xl font-semibold">{RESULT_TITLE[result]}</div>
        {/* 3회 반복·50수처럼 이유를 모르면 버그로 보이는 종료가 있음 */}
        {endReason && (
          <div className="text-sm text-gray-600">
            {END_REASON_LABEL[endReason]}
          </div>
        )}
        {notice && <div className="text-sm text-gray-600">{notice}</div>}
        {rating === 'PENDING' && (
          <div className="h-8 text-sm text-gray-400">점수 반영 중...</div>
        )}
        {rating && rating !== 'PENDING' && <RatingLine {...rating} />}
        <div className="flex w-full gap-4">
          <Button fullWidth text="나가기" onClick={onLeaveRoom} />
        </div>
      </>
    </Modal>
  );
}

// 모달이 뜬 뒤 마운트돼야 올라가는 모습이 보임. 점수를 다 받은 다음에만 그림
function RatingLine({ before, after }: RatingChange) {
  const shown = useCountUp(before, after);
  const delta = after - before;

  if (delta === 0) {
    return <div className="h-8 text-sm text-gray-500">점수 변동 없음</div>;
  }

  return (
    <div className="flex h-8 items-baseline gap-2">
      <span className="text-sm text-gray-500">MMR</span>
      <span className="text-2xl font-semibold tabular-nums">
        {shown.toLocaleString()}
      </span>
      <span className="animate-modal-background text-sm font-semibold text-gray-600">
        {delta > 0 ? `+${delta}` : delta}
      </span>
    </div>
  );
}
