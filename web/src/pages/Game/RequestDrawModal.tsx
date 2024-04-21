import { Button, Modal } from '~/components';

type RequestDrawModalProps = {
  visible: boolean;
  onRequestDraw: () => void;
  onClose: () => void;
};

export function RequestDrawModal({
  visible,
  onRequestDraw,
  onClose,
}: RequestDrawModalProps) {
  return (
    <Modal visible={visible}>
      <>
        <div className="text-2xl font-semibold">무승부 요청</div>
        <div className="text-center">
          상대에게 <b>무승부 요청</b>을 보낼까요?
          <br />
          (승낙 시 즉시 무승부 처리됩니다)
        </div>
        <div className="flex w-full gap-4">
          <Button theme="secondary" fullWidth text="취소" onClick={onClose} />
          <Button fullWidth text="요청" onClick={onRequestDraw} />
        </div>
      </>
    </Modal>
  );
}
