import { Button, Modal } from '~/components';

type ResponseDrawModalProps = {
  visible: boolean;
  onAcceptDraw: () => void;
  onRejectDraw: () => void;
};

export function ResponseDrawModal({
  visible,
  onAcceptDraw,
  onRejectDraw,
}: ResponseDrawModalProps) {
  return (
    <Modal visible={visible}>
      <>
        <div className="text-2xl font-semibold">상대의 무승부 요청</div>
        <div className="text-center">
          상대가 <b>무승부 요청</b>을 보냈습니다.
          <br />
          승낙하시겠습니까?
        </div>
        <div className="flex w-full gap-4">
          <Button
            theme="secondary"
            fullWidth
            text="거절"
            onClick={onRejectDraw}
          />
          <Button fullWidth text="승낙" onClick={onAcceptDraw} />
        </div>
      </>
    </Modal>
  );
}
