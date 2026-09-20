import { Avatar, RatedBadge } from '~/components';

type RoomItemProps = {
  roomId: number;
  title: string;
  hostNickname: string;
  hostImageUrl: string | null;
  hostRating: number;
  rated: boolean;
  ongoing?: boolean;
  onJoinRoom: (roomId: number) => void;
};

export function RoomItem({
  roomId,
  title,
  hostNickname,
  hostImageUrl,
  hostRating,
  rated,
  ongoing = false,
  onJoinRoom,
}: RoomItemProps) {
  return (
    <li
      className={`flex h-20 items-center gap-2 rounded-xl border px-4 py-3 ${ongoing ? 'border-gray-300 bg-gray-300' : 'cursor-pointer border-gray-300 bg-gray-50 hover:bg-white active:bg-gray-100'}`}
      onClick={ongoing ? undefined : () => onJoinRoom(roomId)}
    >
      <Avatar nickname={hostNickname} imageUrl={hostImageUrl} size="lg" />
      <div className="flex grow flex-col gap-0.5">
        <div className="flex items-center gap-2">
          <span className="font-semibold">{title}</span>
          <RatedBadge rated={rated} />
        </div>
        <div className="flex gap-2 text-sm">
          {hostNickname}
          <span className="text-gray-500">MMR {hostRating}</span>
        </div>
      </div>
      <span className="font-semibold">{ongoing ? '진행 중' : '대기 중'}</span>
    </li>
  );
}
