import { Status } from './Status';

export function GamePage() {
  return (
    <main className="flex p-4">
      <div className="flex flex-col">
        <Status
          isCurrentUser={true}
          isTurn={true}
          color="WHITE"
          remaining={7}
        />
        <Status
          isCurrentUser={false}
          isTurn={false}
          color="BLACK"
          remaining={5}
        />
      </div>
      <div className="flex flex-col"></div>
      <div className="flex flex-col"></div>
    </main>
  );
}
