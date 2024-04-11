import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Button } from '~/components';
import { QUERY } from '~/lib/queries';
import Undo from '~/assets/icons/undo.svg?react';
import Add from '~/assets/icons/add.svg?react';
import { CreateRoomModal } from './CreateRoomModal';
import { RoomItem } from './RoomItem';

export function RoomListPage() {
  const [showModal, setShowModal] = useState(false);
  const { data: rooms } = useQuery(QUERY.ROOMS);

  const onClickAdd = () => setShowModal(true);

  return (
    <main className="flex grow items-center justify-center p-4 leading-tight">
      {
        <CreateRoomModal
          isShowing={showModal}
          closeModal={() => setShowModal(false)}
        />
      }
      <div className="flex max-w-[40rem] grow flex-col items-center gap-6">
        <h1 className="text-2xl font-semibold">방 목록</h1>
        <div className="flex w-full justify-between gap-4">
          <Link to="/">
            <Button theme="secondary" slim text="이전으로" icon={<Undo />} />
          </Link>
          <Button slim text="방 만들기" icon={<Add />} onClick={onClickAdd} />
        </div>
        <div className="flex max-h-96 w-full flex-col gap-4 overflow-auto">
          {rooms &&
            rooms.map(
              ({
                roomId,
                roomTitle,
                playerCount,
                host,
                hostImageUrl,
                hostScore,
              }) => (
                <RoomItem
                  key={roomId}
                  roomId={roomId}
                  roomTitle={roomTitle}
                  ongoing={playerCount >= 2}
                  hostNickname={host}
                  hostImageUrl={hostImageUrl}
                  hostScore={hostScore}
                />
              )
            )}
        </div>
      </div>
    </main>
  );
}
