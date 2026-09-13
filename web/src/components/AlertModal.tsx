import { Button } from './Button';
import { Modal } from './Modal';

type AlertModalProps = {
  message: string; // 비어 있으면 띄우지 않음
  onClose: () => void;
};

// 서버가 보낸 실패 문구를 그대로 보여주는 용도
// 예전에는 입장 실패나 방 생성 실패가 아무 표시 없이 먹통 클릭이 됐다
export function AlertModal({ message, onClose }: AlertModalProps) {
  return (
    <Modal visible={!!message}>
      <>
        <div className="font-semibold">{message}</div>
        <Button fullWidth text="확인" onClick={onClose} />
      </>
    </Modal>
  );
}
