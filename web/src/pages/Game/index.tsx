import { Status } from './Status';

export function GamePage() {
  return (
    <main className="flex h-full w-full flex-col gap-8 p-8">
      <div className="flex h-full justify-between">
        <div className="flex flex-col gap-1">
          <h1 className="font-phase text-8xl">Phase 1</h1>
          <span className="text-xl font-semibold">돌 배치 단계</span>
        </div>
        <Status
          isCurrentUser={false}
          isTurn={false}
          color="BLACK"
          remaining={5}
        />
      </div>
      <div className="flex grow flex-col items-center">
        <div></div>
        <span>빈 곳에 돌을 배치하세요.</span>
      </div>
      <div className="flex">
        <Status
          isCurrentUser={true}
          isTurn={true}
          color="WHITE"
          remaining={7}
        />
      </div>
    </main>
  );
}
