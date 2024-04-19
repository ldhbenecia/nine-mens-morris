import { Button, Modal } from '~/components';

type WithdrawModalProps = {
  visible: boolean;
  onWithdraw: () => void;
  onClose: () => void;
};

export function WithdrawModal({
  visible,
  onWithdraw,
  onClose,
}: WithdrawModalProps) {
  return (
    <Modal visible={visible}>
      <>
        <div className="text-2xl font-semibold">기권하시겠습니까?</div>
        <div>패배 처리되며 점수가 떨어집니다.</div>
        <div className="flex w-full gap-4">
          <Button theme="secondary" fullWidth text="취소" onClick={onClose} />
          <Button fullWidth text="확인" onClick={onWithdraw} />
        </div>
      </>
    </Modal>
  );
}
