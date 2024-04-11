import { Button, KakaoButton, Modal } from '~/components';

type LoginModalProps = {
  isShowing: boolean;
  closeModal: () => void;
};

export function LoginModal({ isShowing, closeModal }: LoginModalProps) {
  return (
    <Modal isShowing={isShowing}>
      <>
        <div className="font-semibold">로그인이 필요합니다.</div>
        <div className="flex w-full flex-col gap-4">
          <KakaoButton />
          <Button theme="secondary" text="취소" onClick={closeModal} />
        </div>
      </>
    </Modal>
  );
}
