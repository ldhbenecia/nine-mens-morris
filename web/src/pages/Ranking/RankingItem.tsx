import { useQuery } from '@tanstack/react-query';
import Medal from '~/assets/icons/medal.svg?react';
import Person from '~/assets/icons/person.svg?react';
import { QUERY } from '~/lib/queries';

type RankingItemProps = {
  nickname: string;
  score: number;
  rank: number;
};

export function RankingItem({ nickname, score, rank }: RankingItemProps) {
  const { data: currentUser } = useQuery(QUERY.CURRENT_USER);

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
        <img src="" alt={nickname} className="h-12 w-12 rounded-full" />
        {nickname}
        {currentUser && currentUser.nickname === nickname && (
          <span className="text-sm text-gray-500">나</span>
        )}
      </div>
      <span className="font-semibold">{score.toLocaleString()}점</span>
    </div>
  );
}
