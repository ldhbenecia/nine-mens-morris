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
  order: number; // 목록에서의 순서. 위에서부터 차례로 나타나게 함
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
  order,
}: RankingItemProps) {
  const { data: currentUser } = useQuery(QUERY.CURRENT_USER);
  const isMe = currentUser?.userId === userId;

  return (
    // 눌러도 아무 일이 없어 커서·눌림 표시를 두지 않음
    // 순차 등장은 앞 10줄까지만. 긴 목록 끝까지 기다리게 하지 않음
    <div
      className={`flex animate-rise-in items-center gap-3 rounded-xl border px-3 py-3 sm:gap-4 sm:px-4 ${isMe ? 'border-gray-800 bg-white' : 'border-gray-300 bg-gray-50'}`}
      style={{ animationDelay: `${Math.min(order, 10) * 40}ms` }}
    >
      <div className="flex shrink-0 items-center gap-1 sm:min-w-20">
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
      {/* 좁은 화면에서 닉네임이 글자 단위로 꺾이지 않게 줄임표로 자름 */}
      <div className="flex min-w-0 grow items-center gap-2">
        <Avatar nickname={nickname} imageUrl={imageUrl} size="lg" />
        <span className="truncate">{nickname}</span>
        {isMe && <span className="shrink-0 text-sm text-gray-500">(나)</span>}
      </div>
      <div className="flex shrink-0 flex-col items-end gap-0.5">
        <div className="flex items-center gap-2 text-nowrap">
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
