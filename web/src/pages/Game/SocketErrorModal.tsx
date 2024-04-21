import { Button, Modal } from '~/components';

type SocketErrorModalProps = { visible: boolean; onLeaveRoom: () => void };

export function SocketErrorModal({
  visible,
  onLeaveRoom,
}: SocketErrorModalProps) {
  return (
    <Modal visible={visible}>
      <>
        <div className="text-2xl font-semibold">알림</div>
        <div>상대방과의 연결이 끊겼습니다.</div>
        <div className="flex w-full gap-4">
          <Button fullWidth text="확인" onClick={onLeaveRoom} />
        </div>
      </>
    </Modal>
  );
}
