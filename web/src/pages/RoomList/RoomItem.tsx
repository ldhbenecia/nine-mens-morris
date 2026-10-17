import { Avatar, RatedBadge } from '~/components';
import { Room } from '~/lib/types';

type RoomItemProps = {
  room: Room;
  onJoinRoom: (roomId: number) => void;
};

// 두 명이 다 찼어도 판이 끝났으면 진행 중이 아님. 결과를 보고 아직 나가지 않은 상태
const statusLabel = (playing: boolean, full: boolean) => {
  if (playing) return '진행 중';
  if (full) return '가득 참';
  return '대기 중';
};

export function RoomItem({ room, onJoinRoom }: RoomItemProps) {
  const {
    roomId,
    title,
    hostNickname,
    hostImageUrl,
    hostRating,
    hostVisitor,
    rated,
    playing,
  } = room;
  const full = room.playerCount >= 2;
  const joinable = !playing && !full;

  return (
    <li
      className={`flex h-20 items-center gap-2 rounded-xl border px-4 py-3 ${joinable ? 'cursor-pointer border-gray-300 bg-gray-50 hover:bg-white active:bg-gray-100' : 'border-gray-300 bg-gray-300'}`}
      onClick={joinable ? () => onJoinRoom(roomId) : undefined}
    >
      <Avatar nickname={hostNickname} imageUrl={hostImageUrl} size="lg" />
      <div className="flex grow flex-col gap-0.5">
        <div className="flex items-center gap-2">
          <span className="font-semibold">{title}</span>
          <RatedBadge rated={rated} />
        </div>
        <div className="flex gap-2 text-sm">
          {hostNickname}
          {/* 게스트의 MMR 은 움직이지 않는 값이라 숫자 대신 신분을 보여줌 */}
          <span className="text-gray-500">
            {hostVisitor ? '게스트' : `MMR ${hostRating}`}
          </span>
        </div>
      </div>
      <span className="font-semibold">{statusLabel(playing, full)}</span>
    </li>
  );
}
