import { Button } from '~/components/Button';
import { KakaoButton } from '~/components/KakaoButton';
import { Modal } from '~/components/Modal';

type LoginModalProps = {
  closeModal: () => void;
};

export function LoginModal({ closeModal }: LoginModalProps) {
  return (
    <Modal>
      <>
        <div className="text-lg font-semibold">로그인이 필요합니다.</div>
        <div className="flex w-full flex-col gap-4">
          <KakaoButton />
          <Button theme="secondary" text="취소" onClick={closeModal} />
        </div>
      </>
    </Modal>
  );
}
