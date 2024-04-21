import { Button, Modal } from '~/components';

type GameResultModalProps = {
  visible: boolean;
  result: 'WIN' | 'LOSS' | 'DRAW';
  score?: number;
  onLeaveRoom: () => void;
};

export function GameResultModal({
  visible,
  result,
  score,
  onLeaveRoom,
}: GameResultModalProps) {
  return (
    <Modal visible={visible}>
      <>
        <div className="text-2xl font-semibold">
          {result === 'WIN'
            ? '승리했습니다!'
            : result === 'LOSS'
              ? '패배했습니다...'
              : '무승부!'}
        </div>
        {score && (
          <div>
            점수 {score > 0 ? '+' : '-'}
            {score}
          </div>
        )}
        <div className="flex w-full gap-4">
          <Button fullWidth text="확인" onClick={onLeaveRoom} />
        </div>
      </>
    </Modal>
  );
}
