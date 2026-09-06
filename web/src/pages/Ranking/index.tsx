import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Button } from '~/components';
import Undo from '~/assets/icons/undo.svg?react';
import { QUERY } from '~/lib/queries';
import { RankingItem } from './RankingItem';

export function RankingPage() {
  const { data: ranks } = useQuery(QUERY.RANKS);

  return (
    <main className="flex grow items-center justify-center p-4 leading-tight">
      <div className="flex max-w-[40rem] grow flex-col items-center gap-6">
        <h1 className="text-2xl font-semibold">사용자 랭킹</h1>
        <div className="flex w-full justify-between gap-4">
          <Link to="/">
            <Button theme="secondary" slim text="이전으로" icon={<Undo />} />
          </Link>
        </div>
        <div className="flex w-full flex-col gap-2">
          {ranks &&
            ranks.map((entry) => (
              <RankingItem
                key={entry.userId}
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
