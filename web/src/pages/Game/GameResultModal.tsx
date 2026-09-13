import { Button, Modal } from '~/components';

type GameResultModalProps = {
  visible: boolean;
  result: 'WIN' | 'LOSS' | 'DRAW';
  notice?: string; // 상대 연결 끊김처럼 승패 말고 더 알려줄 것이 있을 때
  onLeaveRoom: () => void;
};

export function GameResultModal({
  visible,
  result,
  notice,
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
        {notice && <div className="text-sm text-gray-600">{notice}</div>}
        <div className="flex w-full gap-4">
          <Button fullWidth text="나가기" onClick={onLeaveRoom} />
        </div>
      </>
    </Modal>
  );
}
