import { Button, Modal } from '~/components';

type NoRoomAlert = {
  visible: boolean;
  onClose: () => void;
};

export function NoRoomAlert({ visible, onClose }: NoRoomAlert) {
  return (
    <Modal visible={visible}>
      <>
        <div className="font-semibold">이미 삭제된 방입니다.</div>
        <Button fullWidth text="확인" onClick={onClose} />
      </>
    </Modal>
  );
}
