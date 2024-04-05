type RoomItemProps = {
  title: string;
  ongoing?: boolean;
  host: { username: string; ranking: number; profileImageSrc: string };
};

export function RoomItem({
  title,
  ongoing = false,
  host: { username, ranking, profileImageSrc },
}: RoomItemProps) {
  return (
    <div
      className={`flex items-center gap-2 rounded-2xl border p-4 ${ongoing ? 'border-gray-300 bg-gray-300' : 'border-gray-300 bg-gray-50'}`}
    >
      <img
        src={profileImageSrc}
        alt={username}
        className="h-12 w-12 rounded-full"
      />
      <div className="flex grow flex-col gap-1">
        <span className="text-lg font-semibold">{title}</span>
        <div className="flex gap-2">
          {username}
          <span className="text-gray-500">랭킹 {ranking}위</span>
        </div>
      </div>
      <span className="text-lg font-semibold">
        {ongoing ? '진행 중' : '대기 중'}
      </span>
    </div>
  );
}
