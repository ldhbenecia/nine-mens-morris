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
    <main className="flex grow flex-col gap-4 p-8">
      {showModal && <WithdrawModal closeModal={() => setShowModal(false)} />}
      <div className="flex justify-between">
        <div className="z-20 flex flex-col">
          <h1 className="font-phase text-7xl">Phase 1</h1>
          <span className="text-lg font-semibold">돌 배치 단계</span>
        </div>
        <Status
          isCurrentUser={false}
          isTurn={false}
          color="BLACK"
          remaining={5}
        />
      </div>
      <Board />
      <div className="flex justify-center">빈 지점에 돌을 배치하세요.</div>
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
