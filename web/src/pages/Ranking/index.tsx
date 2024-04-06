import { Link } from 'react-router-dom';
import { Button } from '~/components/Button';
import Undo from '~/assets/icons/undo.svg?react';
import { RankingItem } from './RankingItem';

export function RankingPage() {
  return (
    <main className="flex h-full items-center justify-center leading-tight">
      <div className="flex max-w-[40rem] grow flex-col items-center gap-6">
        <h1 className="text-2xl font-semibold">사용자 랭킹</h1>
        <div className="flex w-full justify-between gap-4">
          <Link to="/">
            <Button theme="secondary" slim text="이전으로" icon={<Undo />} />
          </Link>
        </div>
        <div className="flex w-full flex-col">
          <RankingItem
            rank={123}
            user={{
              username: 'js43og',
              ranking: 12,
              profileImageSrc:
                'https://avatars.githubusercontent.com/u/50646827?s=96&v=4',
            }}
            score={1000}
            wins={10}
            losses={5}
            isCurrentUser
          />
        </div>
        <div className="flex max-h-96 w-full flex-col gap-4 overflow-auto">
          <RankingItem
            rank={1}
            user={{
              username: 'js43og',
              ranking: 12,
              profileImageSrc:
                'https://avatars.githubusercontent.com/u/50646827?s=96&v=4',
            }}
            score={1000}
            wins={10}
            losses={5}
          />
          <RankingItem
            rank={2}
            user={{
              username: 'js43og',
              ranking: 12,
              profileImageSrc:
                'https://avatars.githubusercontent.com/u/50646827?s=96&v=4',
            }}
            score={1000}
            wins={10}
            losses={5}
          />
          <RankingItem
            rank={3}
            user={{
              username: 'js43og',
              ranking: 12,
              profileImageSrc:
                'https://avatars.githubusercontent.com/u/50646827?s=96&v=4',
            }}
            score={1000}
            wins={10}
            losses={5}
          />
          <RankingItem
            rank={4}
            user={{
              username: 'js43og',
              ranking: 12,
              profileImageSrc:
                'https://avatars.githubusercontent.com/u/50646827?s=96&v=4',
            }}
            score={1000}
            wins={10}
            losses={5}
          />
          <RankingItem
            rank={5}
            user={{
              username: 'js43og',
              ranking: 12,
              profileImageSrc:
                'https://avatars.githubusercontent.com/u/50646827?s=96&v=4',
            }}
            score={1000}
            wins={10}
            losses={5}
          />
        </div>
      </div>
    </main>
  );
}
