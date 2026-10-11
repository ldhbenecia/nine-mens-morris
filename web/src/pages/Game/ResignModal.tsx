import { Button, Modal } from '~/components';

type ResignModalProps = {
  visible: boolean;
  rated: boolean;
  onResign: () => void;
  onClose: () => void;
};

// 일반전은 전적도 점수도 남지 않고, 랭크전도 10수 미만이면 점수는 그대로임
// 늘 "점수가 내려갑니다" 라고 하면 사실과 달라 기권을 망설이게 만듦
export function ResignModal({
  visible,
  rated,
  onResign,
  onClose,
}: ResignModalProps) {
  return (
    <Modal visible={visible}>
      <>
        <div className="text-2xl font-semibold">기권하시겠습니까?</div>
        {rated ? (
          <div className="text-center">
            패배로 기록됩니다
            <div className="text-sm text-gray-500">
              10수 미만에서 끝나면 점수는 변하지 않습니다
            </div>
          </div>
        ) : (
          <div>일반전이라 전적과 점수에는 반영되지 않습니다</div>
        )}
        <div className="flex w-full gap-4">
          <Button theme="secondary" fullWidth text="취소" onClick={onClose} />
          <Button fullWidth text="기권" onClick={onResign} />
        </div>
      </>
    </Modal>
  );
}
