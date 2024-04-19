import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Button } from '~/components';
import { QUERY } from '~/lib/queries';
import Undo from '~/assets/icons/undo.svg?react';
import Add from '~/assets/icons/add.svg?react';
import { CreateRoomModal } from './CreateRoomModal';
import { RoomItem } from './RoomItem';
import { NoRoomAlert } from './NoRoomAlert';

export function RoomListPage() {
  const [showCreateRoomModal, setShowCreateRoomModal] = useState(false);
  const [showNoRoomAlert, setShowNoRoomAlert] = useState(false);
  const { data: rooms, refetch } = useQuery(QUERY.ROOMS);
  const navigate = useNavigate();

  const onJoinRoom = async (roomId: number) => {
    const { data: currentRooms } = await refetch();
    if (currentRooms?.find((room) => room.roomId === roomId)) {
      return navigate(`/game/${roomId}`);
    }

    setShowNoRoomAlert(true);
  };

  const onClickCreateRoom = () => setShowCreateRoomModal(true);

  return (
    <main className="flex grow items-center justify-center p-4 leading-tight">
      <CreateRoomModal
        visible={showCreateRoomModal}
        onClose={() => setShowCreateRoomModal(false)}
      />
      <NoRoomAlert
        visible={showNoRoomAlert}
        onClose={() => setShowNoRoomAlert(false)}
      />
      <div className="flex max-w-[40rem] grow flex-col items-center gap-6">
        <h1 className="text-2xl font-semibold">방 목록</h1>
        <div className="flex w-full justify-between gap-4">
          <Link to="/">
            <Button theme="secondary" slim text="이전으로" icon={<Undo />} />
          </Link>
          <Button
            slim
            text="방 만들기"
            icon={<Add />}
            onClick={onClickCreateRoom}
          />
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
                  hostNickname={host}
                  hostImageUrl={hostImageUrl}
                  hostScore={hostScore}
                  ongoing={playerCount >= 2}
                  onJoinRoom={onJoinRoom}
                />
              )
            )}
        </div>
      </div>
    </main>
  );
}
