import { Link } from 'react-router-dom';
import { Button } from '~/components/Button';
import Undo from '~/assets/icons/undo.svg?react';
import { RankingItem } from './RankingItem';

export function RankingPage() {
  return (
    <main className="flex grow items-center justify-center p-4 leading-tight">
      <div className="flex max-w-[40rem] grow flex-col items-center gap-6">
        <h1 className="text-2xl font-semibold">사용자 랭킹</h1>
        <div className="flex w-full justify-between gap-4">
          <Link to="/">
            <Button theme="secondary" slim text="이전으로" icon={<Undo />} />
          </Link>
        </div>
        <div className="flex w-full flex-col">
          <RankingItem
            user={{
              username: 'js43og',
              rank: 123,
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
            user={{
              username: 'js43og',
              rank: 1,
              profileImageSrc:
                'https://avatars.githubusercontent.com/u/50646827?s=96&v=4',
            }}
            score={12400}
            wins={240}
            losses={20}
          />
          <RankingItem
            user={{
              username: 'js43og',
              rank: 2,
              profileImageSrc:
                'https://avatars.githubusercontent.com/u/50646827?s=96&v=4',
            }}
            score={7280}
            wins={10}
            losses={5}
          />
          <RankingItem
            user={{
              username: 'js43og',
              rank: 3,
              profileImageSrc:
                'https://avatars.githubusercontent.com/u/50646827?s=96&v=4',
            }}
            score={1000}
            wins={10}
            losses={5}
          />
          <RankingItem
            user={{
              username: 'js43og',
              rank: 4,
              profileImageSrc:
                'https://avatars.githubusercontent.com/u/50646827?s=96&v=4',
            }}
            score={1000}
            wins={10}
            losses={5}
          />
          <RankingItem
            user={{
              username: 'js43og',
              rank: 5,
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
