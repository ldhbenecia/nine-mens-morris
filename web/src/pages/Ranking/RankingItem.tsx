import Medal from '~/assets/icons/medal.svg?react';
import Person from '~/assets/icons/person.svg?react';
import { User } from '~/types';

type RankingItemProps = {
  user: User;
  score: number;
  wins: number;
  losses: number;
  isCurrentUser?: boolean;
};

export function RankingItem({
  user: { username, rank, profileImageSrc },
  score,
  wins,
  losses,
  isCurrentUser = false,
}: RankingItemProps) {
  return (
    <div
      className={`flex items-center gap-4 rounded-xl border border-gray-300 bg-gray-50 px-4 py-3 hover:bg-white active:bg-gray-100`}
    >
      <div className="flex min-w-20 items-center gap-1">
        {rank <= 3 ? (
          <Medal
            className={
              rank === 1
                ? `fill-amber-500`
                : rank === 2
                  ? 'fill-slate-500'
                  : 'fill-amber-700'
            }
          />
        ) : (
          <Person />
        )}
        <span className="font-semibold">{rank}위</span>
      </div>
      <div className="flex grow items-center gap-2">
        <img
          src={profileImageSrc}
          alt={username}
          className="h-12 w-12 rounded-full"
        />
        {username}
        {isCurrentUser && <span className="text-sm text-gray-500">나</span>}
      </div>
      <div className="flex flex-col items-end gap-1">
        <span className="font-semibold">{score.toLocaleString()}점</span>
        <span className="flex text-sm text-gray-500">
          {wins}승 {losses}패
        </span>
      </div>
    </div>
  );
}
