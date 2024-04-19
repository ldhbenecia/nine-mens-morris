type RoomItemProps = {
  roomId: number;
  roomTitle: string;
  hostNickname: string;
  hostImageUrl: string;
  hostScore: number;
  ongoing?: boolean;
  onJoinRoom: (roomId: number) => void;
};

export function RoomItem({
  roomId,
  roomTitle,
  hostNickname,
  hostImageUrl,
  hostScore,
  ongoing = false,
  onJoinRoom,
}: RoomItemProps) {
  return (
    <div
      className={`flex items-center gap-2 rounded-xl border px-4 py-3 ${ongoing ? 'border-gray-300 bg-gray-300' : 'cursor-pointer border-gray-300 bg-gray-50 hover:bg-white active:bg-gray-100'}`}
      onClick={ongoing ? undefined : () => onJoinRoom(roomId)}
    >
      <img
        src={hostImageUrl}
        alt={hostNickname}
        className="h-12 w-12 rounded-full"
      />
      <div className="flex grow flex-col gap-0.5">
        <span className="font-semibold">{roomTitle}</span>
        <div className="flex gap-2 text-sm">
          {hostNickname}
          <span className="text-gray-500">레이팅 {hostScore}점</span>
        </div>
      </div>
      <span className="font-semibold">{ongoing ? '진행 중' : '대기 중'}</span>
    </div>
  );
}
