type RoomItemProps = {
  title: string;
  ongoing?: boolean;
  host: { username: string; rank: number; profileImageSrc: string };
};

export function RoomItem({
  title,
  ongoing = false,
  host: { username, rank, profileImageSrc },
}: RoomItemProps) {
  return (
    <div
      className={`flex items-center gap-2 rounded-xl border px-4 py-3 ${ongoing ? 'border-gray-300 bg-gray-300' : 'border-gray-300 bg-gray-50 hover:bg-white active:bg-gray-100'}`}
    >
      <img
        src={profileImageSrc}
        alt={username}
        className="h-12 w-12 rounded-full"
      />
      <div className="flex grow flex-col gap-1">
        <span className="font-semibold">{title}</span>
        <div className="flex gap-2 text-sm">
          {username}
          <span className="text-gray-500">랭킹 {rank}위</span>
        </div>
      </div>
      <span className="font-semibold">{ongoing ? '진행 중' : '대기 중'}</span>
    </div>
  );
}
