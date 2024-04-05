import { Button } from '~/components/Button';
import { Modal } from '~/components/Modal';

type CreateRoomProps = {
  closeModal: () => void;
};

export function CreateRoomModal({ closeModal }: CreateRoomProps) {
  return (
    <Modal>
      <>
        <div className="font-semibold">방 제목을 입력해 주세요.</div>
        <input
          type="text"
          className="flex w-full rounded-md border border-gray-400 p-3"
        />
        <div className="flex w-full gap-4">
          <Button
            theme="secondary"
            fullWidth
            text="취소"
            onClick={closeModal}
          />
          <Button fullWidth text="확인" onClick={() => {}} />
        </div>
      </>
    </Modal>
  );
}
