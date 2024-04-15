import { Button, Modal } from '~/components';

type LogoutModalProps = {
  visible: boolean;
  onLogout: () => void;
  onClose: () => void;
};

export function LogoutModal({ visible, onLogout, onClose }: LogoutModalProps) {
  return (
    <Modal visible={visible}>
      <>
        <div className="font-semibold">로그아웃할까요?</div>
        <div className="flex w-full gap-4">
          <Button theme="secondary" text="취소" onClick={onClose} fullWidth />
          <Button text="로그아웃" onClick={onLogout} fullWidth />
        </div>
      </>
    </Modal>
  );
}
