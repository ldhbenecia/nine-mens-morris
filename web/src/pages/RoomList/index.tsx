import { useState } from 'react';
import { CreateRoomModal } from './CreateRoomModal';
import { Button } from '~/components/Button';
import Undo from '~/assets/icons/undo.svg?react';
import Add from '~/assets/icons/add.svg?react';
import { RoomItem } from './RoomItem';
import { Link } from 'react-router-dom';

export function RoomListPage() {
  const [showModal, setShowModal] = useState(false);

  const onClickAdd = () => setShowModal(true);

  return (
    <main className="flex grow items-center justify-center p-4 leading-tight">
      {showModal && <CreateRoomModal closeModal={() => setShowModal(false)} />}
      <div className="flex max-w-[40rem] grow flex-col items-center gap-6">
        <h1 className="text-2xl font-semibold">방 목록</h1>
        <div className="flex w-full justify-between gap-4">
          <Link to="/">
            <Button theme="secondary" slim text="이전으로" icon={<Undo />} />
          </Link>
          <Button slim text="방 만들기" icon={<Add />} onClick={onClickAdd} />
        </div>
        <div className="flex max-h-96 w-full flex-col gap-4 overflow-auto">
          <RoomItem
            title="초보만 오세요"
            ongoing
            host={{
              username: 'js43og',
              rank: 12,
              profileImageSrc:
                'https://avatars.githubusercontent.com/u/50646827?s=96&v=4',
            }}
          />
          <RoomItem
            title="초보만 오세요"
            host={{
              username: 'js43o',
              rank: 12,
              profileImageSrc:
                'https://avatars.githubusercontent.com/u/50646827?s=96&v=4',
            }}
          />
          <RoomItem
            title="초보만 오세요"
            host={{
              username: 'js43o',
              rank: 12,
              profileImageSrc:
                'https://avatars.githubusercontent.com/u/50646827?s=96&v=4',
            }}
          />
          <RoomItem
            title="초보만 오세요"
            host={{
              username: 'js43o',
              rank: 12,
              profileImageSrc:
                'https://avatars.githubusercontent.com/u/50646827?s=96&v=4',
            }}
          />
          <RoomItem
            title="초보만 오세요"
            host={{
              username: 'js43o',
              rank: 12,
              profileImageSrc:
                'https://avatars.githubusercontent.com/u/50646827?s=96&v=4',
            }}
          />
        </div>
      </div>
    </main>
  );
}
