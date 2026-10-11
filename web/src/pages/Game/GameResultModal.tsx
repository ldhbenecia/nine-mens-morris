import { Button, Modal } from '~/components';
import { END_REASON_LABEL } from '~/lib/endReason';
import { GameResult } from '~/lib/gameResult';
import { EndReason } from '~/lib/types';

type GameResultModalProps = {
  visible: boolean;
  result: GameResult;
  endReason: EndReason | null;
  notice?: string; // 상대 연결 끊김처럼 종료 사유 말고 더 알려줄 것이 있을 때
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
        <div className="flex w-full gap-4">
          <Button fullWidth text="나가기" onClick={onLeaveRoom} />
        </div>
      </>
    </Modal>
  );
}
