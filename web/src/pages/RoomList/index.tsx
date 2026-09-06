import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Button } from '~/components';
import { QUERY } from '~/lib/queries';
import Undo from '~/assets/icons/undo.svg?react';
import Refresh from '~/assets/icons/refresh.svg?react';
import { CreateRoomModal } from './CreateRoomModal';
import { RoomItem } from './RoomItem';
import { NoRoomAlert } from './NoRoomAlert';
import { CreateRoomButton } from './CreateRoomButton';
import { useJoinRoom } from '~/hooks';

export function RoomListPage() {
  const [showCreateRoomModal, setShowCreateRoomModal] = useState(false);
  const [showNoRoomAlert, setShowNoRoomAlert] = useState(false);
  const [refreshed, setRefreshed] = useState(true);
  const { data: rooms, refetch } = useQuery(QUERY.ROOMS);
  const { mutate: joinRoom } = useJoinRoom();

  // 서버에 실제로 입장 요청을 보낸다
  // 예전에는 목록에 있는지만 확인하고 화면을 넘겨서, 방이 가득 찼거나
  // 사라진 경우를 알 수 없었다
  const onJoinRoom = async (roomId: number) => {
    const { data: currentRooms } = await refetch();
    if (!currentRooms?.find((room) => room.roomId === roomId)) {
      return setShowNoRoomAlert(true);
    }

    joinRoom(roomId);
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
              .map((room) => (
                <RoomItem
                  key={room.roomId}
                  roomId={room.roomId}
                  title={room.title}
                  hostNickname={room.hostNickname}
                  hostImageUrl={room.hostImageUrl}
                  hostRating={room.hostRating}
                  ongoing={room.playing || room.playerCount >= 2}
                  onJoinRoom={onJoinRoom}
                />
              ))}
        </ul>
      </div>
    </main>
  );
}
