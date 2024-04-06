import { useState } from 'react';
import { Board } from './Board';
import { Status } from './Status';
import { WithdrawModal } from './WithdrawModal';

export function GamePage() {
  const [showModal, setShowModal] = useState(false);

  const onWithdraw = () => {
    setShowModal(true);
  };

  return (
    <main className="flex h-full w-full flex-col gap-8 p-8">
      {showModal && <WithdrawModal closeModal={() => setShowModal(false)} />}
      <div className="flex justify-between">
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
      <div className="flex grow items-center justify-center">
        <Board />
      </div>
      <div className="flex">
        <Status
          isCurrentUser={true}
          isTurn={true}
          color="WHITE"
          remaining={7}
          onWithdraw={onWithdraw}
        />
      </div>
    </main>
  );
}
