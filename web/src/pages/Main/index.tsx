import { useState } from 'react';
import { Button } from '~/components/Button';
import { LoginModal } from './LoginModal';
import { Link } from 'react-router-dom';

export function MainPage() {
  const [showModal, setShowModal] = useState(false);

  return (
    <main className="flex h-full flex-col items-center justify-center gap-12">
      {showModal && <LoginModal closeModal={() => setShowModal(false)} />}
      <div className="flex flex-col items-center gap-2">
        <h1 className="font-title text-4xl">Nine Men&apos;s Morris</h1>
        <h2 className="text-xl font-light tracking-[0.75rem] text-gray-500">
          나인 멘스 모리스
        </h2>
      </div>
      <div className="flex w-60 flex-col gap-4">
        <Button text="게임 시작" onClick={() => setShowModal(true)} />
        <Link to="/ranking" className="flex w-full flex-col">
          <Button theme="secondary" text="랭킹 보기" onClick={() => {}} />
        </Link>
      </div>
    </main>
  );
}
