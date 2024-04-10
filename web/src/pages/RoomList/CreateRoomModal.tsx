import { useState } from 'react';
import { Button } from '~/components/Button';
import { Modal } from '~/components/Modal';
import { useJoinRoom, useCreateRoom } from '~/hooks/useMutations';

type CreateRoomProps = {
  closeModal: () => void;
};

export function CreateRoomModal({ closeModal }: CreateRoomProps) {
  const [roomTitle, setRoomTitle] = useState('');
  const { mutate } = useCreateRoom();

  const onChangeInput = (e: React.ChangeEvent<HTMLInputElement>) =>
    setRoomTitle(e.target.value);

  const onCreateRoom = () => {
    if (!roomTitle) return;

    mutate(roomTitle);
  };

  return (
    <Modal>
      <>
        <div className="font-semibold">방 제목을 입력해 주세요.</div>
        <input
          type="text"
          className="flex w-full rounded-md border border-gray-400 p-3"
          value={roomTitle}
          onChange={onChangeInput}
        />
        <div className="flex w-full gap-4">
          <Button
            theme="secondary"
            fullWidth
            text="취소"
            onClick={closeModal}
          />
          <Button fullWidth text="확인" onClick={onCreateRoom} />
        </div>
      </>
    </Modal>
  );
}
