import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Button } from '~/components';
import Undo from '~/assets/icons/undo.svg?react';
import { QUERY } from '~/lib/queries';
import { TIER_LABEL } from '~/lib/tier';
import { RankingItem } from './RankingItem';

export function RankingPage() {
  const { data: ranks } = useQuery(QUERY.RANKS);
  const { data: currentUser } = useQuery(QUERY.CURRENT_USER);

  return (
    <main className="flex grow items-center justify-center p-4 leading-tight">
      <div className="flex max-w-160 grow flex-col items-center gap-6">
        <div className="flex flex-col items-center gap-1">
          <h1 className="text-2xl font-semibold">사용자 랭킹</h1>
          {/* 목록이 길면 내 줄을 찾기 어려움. 게스트는 랭킹에 오르지 않음 */}
          {currentUser?.rank && (
            <span className="text-sm text-gray-500">
              내 순위 <b className="text-gray-800">{currentUser.rank}위</b> ·{' '}
              {TIER_LABEL[currentUser.tier]} {currentUser.mmr.toLocaleString()}
            </span>
          )}
        </div>
        <div className="flex w-full justify-between gap-4">
          <Link to="/">
            <Button theme="secondary" slim text="이전으로" icon={<Undo />} />
          </Link>
        </div>
        {/* 회원끼리 판을 둬야 랭킹이 생김. 게스트는 랭킹에 오르지 않음 */}
        {ranks?.length === 0 && (
          <div className="flex w-full flex-col items-center gap-1 rounded-xl border border-dashed border-gray-300 py-12 text-center">
            <span className="font-semibold">
              아직 랭킹에 오른 사람이 없습니다
            </span>
            <span className="text-sm text-gray-500">
              카카오로 로그인해 랭크전을 두면 이곳에 이름이 오릅니다
            </span>
          </div>
        )}
        <div className="flex w-full flex-col gap-2">
          {ranks &&
            ranks.map((entry, order) => (
              <RankingItem
                key={entry.userId}
                order={order}
                userId={entry.userId}
                nickname={entry.nickname}
                imageUrl={entry.imageUrl}
                mmr={entry.mmr}
                tier={entry.tier}
                wins={entry.wins}
                losses={entry.losses}
                rank={entry.rank}
              />
            ))}
        </div>
      </div>
    </main>
  );
}
