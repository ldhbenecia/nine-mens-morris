import { useQuery } from '@tanstack/react-query';
import Medal from '~/assets/icons/medal.svg?react';
import Person from '~/assets/icons/person.svg?react';
import { Avatar } from '~/components';
import { QUERY } from '~/lib/queries';
import { Tier } from '~/lib/types';
import { TIER_LABEL, TIER_STYLE } from '~/lib/tier';

type RankingItemProps = {
  userId: number;
  nickname: string;
  imageUrl: string | null;
  mmr: number;
  tier: Tier;
  wins: number;
  losses: number;
  rank: number;
};

export function RankingItem({
  userId,
  nickname,
  imageUrl,
  mmr,
  tier,
  wins,
  losses,
  rank,
}: RankingItemProps) {
  const { data: currentUser } = useQuery(QUERY.CURRENT_USER);

  return (
    <div
      className={`flex cursor-pointer items-center gap-4 rounded-xl border border-gray-300 bg-gray-50 px-4 py-3 hover:bg-white active:bg-gray-100`}
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
        <Avatar nickname={nickname} imageUrl={imageUrl} size="lg" />
        {nickname}
        {currentUser?.userId === userId && (
          <span className="text-sm text-gray-500">나</span>
        )}
      </div>
      <div className="flex flex-col items-end gap-0.5">
        <div className="flex items-center gap-2">
          <span className={`text-sm font-semibold ${TIER_STYLE[tier]}`}>
            {TIER_LABEL[tier]}
          </span>
          <span className="font-semibold">{mmr.toLocaleString()}</span>
        </div>
        <span className="text-xs text-gray-500">
          {wins}승 {losses}패
        </span>
      </div>
    </div>
  );
}
