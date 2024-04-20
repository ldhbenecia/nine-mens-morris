import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Button } from '~/components';
import { QUERY } from '~/lib/queries';
import Undo from '~/assets/icons/undo.svg?react';
import Refresh from '~/assets/icons/refresh.svg?react';
import { CreateRoomModal } from './CreateRoomModal';
import { RoomItem } from './RoomItem';
import { NoRoomAlert } from './NoRoomAlert';
import { CreateRoomButton } from './CreateRoomButton';

export function RoomListPage() {
  const [showCreateRoomModal, setShowCreateRoomModal] = useState(false);
  const [showNoRoomAlert, setShowNoRoomAlert] = useState(false);
  const [refreshed, setRefreshed] = useState(true);
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

  const onClickRefresh = () => {
    setRefreshed(false);
    refetch();
    window.setTimeout(() => setRefreshed(true));
  };

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
            theme="secondary"
            slim
            text="새로고침"
            icon={<Refresh className={refreshed ? 'animate-refresh' : ''} />}
            onClick={onClickRefresh}
          />
        </div>
        <ul className="flex w-full flex-col gap-4">
          <CreateRoomButton onClick={onClickCreateRoom} />
          {rooms &&
            rooms
              .slice()
              .reverse()
              .map(
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
        </ul>
      </div>
    </main>
  );
}
