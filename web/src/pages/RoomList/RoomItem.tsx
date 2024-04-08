import { useMutation } from '@tanstack/react-query';
import { MUTATIONS } from '~/lib/mutations';

type RoomItemProps = {
  roomId: number;
  roomTitle: string;
  hostNickname: string;
  ongoing?: boolean;
};

export function RoomItem({
  roomId,
  roomTitle,
  hostNickname,
  ongoing = false,
}: RoomItemProps) {
  const { mutate } = useMutation(MUTATIONS.JOIN_ROOM);

  const onJoinRoom = () => mutate(roomId);

  return (
    <div
      className={`flex items-center gap-2 rounded-xl border px-4 py-3 ${ongoing ? 'border-gray-300 bg-gray-300' : 'border-gray-300 bg-gray-50 hover:bg-white active:bg-gray-100'}`}
      onClick={ongoing ? undefined : onJoinRoom}
    >
      {/* <img
        src={profileImageSrc}
        alt={username}
        className="h-12 w-12 rounded-full"
      /> */}
      <div className="flex grow flex-col gap-1">
        <span className="font-semibold">{roomTitle}</span>
        <div className="flex gap-2 text-sm">
          {hostNickname}
          {/* <span className="text-gray-500">레이팅 {score}점</span> */}
        </div>
      </div>
      <span className="font-semibold">{ongoing ? '진행 중' : '대기 중'}</span>
    </div>
  );
}
