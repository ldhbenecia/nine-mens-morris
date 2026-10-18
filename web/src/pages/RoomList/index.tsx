import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { AlertModal, Button } from '~/components';
import { QUERY } from '~/lib/queries';
import { DESTINATION, ManagedSocket } from '~/lib/socket';
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
  // 사라졌거나 가득 찬 방인지는 서버가 판단함. 실패하면 서버 문구를 띄우고 목록을 다시 받음
  const { mutate: joinRoom } = useJoinRoom(setAlertMessage);

  // 방이 생기거나 사라지면 서버가 로비 토픽으로 알려줌
  // 목록 자체는 REST 로 다시 받음. 소켓으로 목록 전체를 흘리면
  // 접속자 수만큼 같은 데이터가 반복 전송됨
  // 끊겼다 다시 붙거나 서버를 옮긴 사이 놓친 신호가 있을 수 있어 두 번째 연결부터는 목록도 다시 받음
  useEffect(() => {
    const socket = new ManagedSocket();
    let connectedBefore = false;
    socket.start({
      onConnect: (client) => {
        client.subscribe(DESTINATION.LOBBY, () => refetch());
        if (connectedBefore) refetch();
        connectedBefore = true;
      },
    });

    return () => socket.stop();
  }, [refetch]);

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
      <div className="flex max-w-160 grow flex-col items-center gap-6">
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
          {/* 서버가 최근에 만든 방부터 줌. 새 대기방이 위에 와야 입장할 방을 바로 찾음 */}
          {rooms?.map((room) => (
            <RoomItem key={room.roomId} room={room} onJoinRoom={joinRoom} />
          ))}
        </ul>
      </div>
    </main>
  );
}
