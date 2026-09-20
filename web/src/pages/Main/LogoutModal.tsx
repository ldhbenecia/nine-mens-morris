import { Button, Modal } from '~/components';

type LogoutModalProps = {
  visible: boolean;
  visitor: boolean;
  onLogout: () => void;
  onClose: () => void;
};

export function LogoutModal({
  visible,
  visitor,
  onLogout,
  onClose,
}: LogoutModalProps) {
  return (
    <Modal visible={visible}>
      <>
        <div className="font-semibold">로그아웃할까요?</div>
        {/* 게스트는 토큰이 곧 신원이라 버리면 같은 신원으로 돌아올 방법이 없다 */}
        {visitor && (
          <span className="text-center text-xs text-gray-500">
            게스트 신원은 다시 쓸 수 없게 됩니다
          </span>
        )}
        <div className="flex w-full gap-4">
          <Button theme="secondary" text="취소" onClick={onClose} fullWidth />
          <Button text="로그아웃" onClick={onLogout} fullWidth />
        </div>
      </>
    </Modal>
  );
}
