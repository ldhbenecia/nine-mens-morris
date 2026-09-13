import { useState } from 'react';
import { Button, Modal } from '~/components';
import { useCreateRoom } from '~/hooks';

type CreateRoomProps = {
  visible: boolean;
  onClose: () => void;
};

const MAX_TITLE_LENGTH = 30; // 서버의 @Size(max = 30) 와 같은 값

export function CreateRoomModal({ visible, onClose }: CreateRoomProps) {
  const [roomTitle, setRoomTitle] = useState('');
  const [error, setError] = useState('');
  const { mutate } = useCreateRoom(setError);

  const onChangeInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    setError('');
    setRoomTitle(e.target.value);
  };

  const onCreateRoom = () => {
    if (!roomTitle.trim()) return setError('방 제목을 입력해 주세요');

    mutate(roomTitle);
  };

  return (
    <Modal visible={visible}>
      <>
        <div className="font-semibold">방 제목을 입력해 주세요</div>
        <input
          type="text"
          className="flex w-full rounded-md border border-gray-400 p-3"
          value={roomTitle}
          maxLength={MAX_TITLE_LENGTH}
          onChange={onChangeInput}
        />
        {error && <div className="text-sm text-red-600">{error}</div>}
        <div className="flex w-full gap-4">
          <Button theme="secondary" fullWidth text="취소" onClick={onClose} />
          <Button fullWidth text="만들기" onClick={onCreateRoom} />
        </div>
      </>
    </Modal>
  );
}
