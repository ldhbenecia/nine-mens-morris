import { Button, Modal } from '~/components';

type InfoModalProps = {
  text: string;
  onClose: () => void;
};

export function InfoModal({ text, onClose }: InfoModalProps) {
  return (
    <Modal visible={!!text}>
      <>
        <div className="text-2xl font-semibold">알림</div>
        <div>{text}</div>
        <div className="flex w-full gap-4">
          <Button fullWidth text="확인" onClick={onClose} />
        </div>
      </>
    </Modal>
  );
}
