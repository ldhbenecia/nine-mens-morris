import { Button, Modal } from '~/components';

type DrawRejectedModalProps = {
  visible: boolean;
  onClose: () => void;
};

export function DrawRejectedModal({
  visible,
  onClose,
}: DrawRejectedModalProps) {
  return (
    <Modal visible={visible}>
      <>
        <div className="text-2xl font-semibold">알림</div>
        <div>상대가 무승부를 거절했습니다</div>
        <div className="flex w-full gap-4">
          <Button fullWidth text="확인" onClick={onClose} />
        </div>
      </>
    </Modal>
  );
}
