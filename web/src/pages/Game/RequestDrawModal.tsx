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
        <div className="text-2xl font-semibold">무승부 제안</div>
        <div className="text-center">
          상대에게 <b>무승부</b>를 제안할까요?
          <br />
          (상대가 수락하면 바로 무승부로 끝납니다)
        </div>
        <div className="flex w-full gap-4">
          <Button theme="secondary" fullWidth text="취소" onClick={onClose} />
          <Button fullWidth text="제안" onClick={onRequestDraw} />
        </div>
      </>
    </Modal>
  );
}
