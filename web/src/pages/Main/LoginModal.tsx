import { Button, KakaoButton, Modal } from '~/components';

type LoginModalProps = {
  visible: boolean;
  onClose: () => void;
};

export function LoginModal({ visible, onClose }: LoginModalProps) {
  return (
    <Modal visible={visible}>
      <>
        <div className="font-semibold">로그인이 필요합니다.</div>
        <div className="flex w-full flex-col gap-4">
          <KakaoButton />
          <Button theme="secondary" text="취소" onClick={onClose} />
        </div>
      </>
    </Modal>
  );
}
