import { Button, Modal } from '~/components';

type HelpModalProps = {
  visible: boolean;
  onClose: () => void;
};

export function HelpModal({ visible, onClose }: HelpModalProps) {
  return (
    <Modal visible={visible}>
      <>
        <div className="text-2xl font-semibold">도움말</div>
        <div></div>
        <div className="flex w-full gap-4">
          <Button theme="secondary" fullWidth text="이전" onClick={onClose} />
          <Button fullWidth text="다음" onClick={() => {}} />
        </div>
      </>
    </Modal>
  );
}
