import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Client } from '@stomp/stompjs';
import { AlertModal, Button } from '~/components';
import { authHeaders } from '~/lib/auth';
import { QUERY } from '~/lib/queries';
import Undo from '~/assets/icons/undo.svg?react';
import Refresh from '~/assets/icons/refresh.svg?react';
import { CreateRoomModal } from './CreateRoomModal';
import { RoomItem } from './RoomItem';
import { CreateRoomButton } from './CreateRoomButton';
import { useJoinRoom } from '~/hooks';

export function RoomListPage() {
  const [showCreateRoomModal, setShowCreateRoomModal] = useState(false);
  const [alertMessage, setAlertMessage] = useState('');
  const [refreshed, setRefreshed] = useState(true);
  const { data: rooms, refetch } = useQuery(QUERY.ROOMS);
  const { mutate: joinRoom } = useJoinRoom(setAlertMessage);

  // 방이 생기거나 사라지면 서버가 로비 토픽으로 알려줌
  // 목록 자체는 REST 로 다시 받음. 소켓으로 목록 전체를 흘리면
  // 접속자 수만큼 같은 데이터가 반복 전송됨
  useEffect(() => {
    const client = new Client({
      brokerURL: import.meta.env.VITE_SOCKET_URL,
      reconnectDelay: 5000,
      beforeConnect: () => {
        client.connectHeaders = authHeaders();
      },
      onConnect: () => {
        client.subscribe('/topic/lobby', () => refetch());
      },
    });
    client.activate();

    return () => {
      client.deactivate();
    };
  }, [refetch]);

  // 목록에 있는지 먼저 보는 것은 빠른 피드백용일 뿐이고
  // 가득 찼는지 같은 판단은 서버가 한다. 실패하면 서버 문구를 그대로 띄운다
  const onJoinRoom = async (roomId: number) => {
    const { data: currentRooms } = await refetch();
    if (!currentRooms?.find((room) => room.roomId === roomId)) {
      return setAlertMessage('사라진 방입니다');
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
      <AlertModal message={alertMessage} onClose={() => setAlertMessage('')} />
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
                  rated={room.rated}
                  ongoing={room.playing || room.playerCount >= 2}
                  onJoinRoom={onJoinRoom}
                />
              ))}
        </ul>
      </div>
    </main>
  );
}
