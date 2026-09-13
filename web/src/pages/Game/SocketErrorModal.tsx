import { Button, Modal } from '~/components';

type SocketErrorModalProps = { visible: boolean; onLeaveRoom: () => void };

// 내 소켓이 끊겼을 때. 예전에는 상대가 끊긴 경우에만 떠서
// 정작 내 연결이 죽으면 아무 표시 없이 클릭만 먹통이 됐다
export function SocketErrorModal({
  visible,
  onLeaveRoom,
}: SocketErrorModalProps) {
  return (
    <Modal visible={visible}>
      <>
        <div className="text-2xl font-semibold">연결이 끊겼습니다</div>
        <div className="text-sm text-gray-600">
          다시 연결하고 있습니다. 계속되면 나가기를 눌러 주세요
        </div>
        <div className="flex w-full gap-4">
          <Button
            fullWidth
            text="나가기"
            theme="secondary"
            onClick={onLeaveRoom}
          />
        </div>
      </>
    </Modal>
  );
}
