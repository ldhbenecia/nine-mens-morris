import { useState } from 'react';
import { Button } from '~/components/Button';
import { LoginModal } from './LoginModal';
import { Link, useNavigate } from 'react-router-dom';
import { UserInfo } from './UserInfo';
import { User } from '~/types';

export function MainPage() {
  const [showModal, setShowModal] = useState(false);
  const [user, setUser] = useState<User | null>({
    username: 'js43og',
    rank: 123,
    profileImageSrc: 'https://avatars.githubusercontent.com/u/50646827?v=4',
  });
  const navigate = useNavigate();

  const onClickStart = () => (user ? navigate('/rooms') : setShowModal(true));
  const onLogout = () => setUser(null);

  return (
    <main className="flex grow flex-col items-center justify-center gap-12">
      {showModal && <LoginModal closeModal={() => setShowModal(false)} />}
      <UserInfo user={user} onLogout={onLogout} />
      <div className="flex flex-col items-center gap-2">
        <h1 className="font-title text-4xl">Nine Men&apos;s Morris</h1>
        <h2 className="text-xl font-light tracking-[0.75rem] text-gray-500">
          나인 멘스 모리스
        </h2>
      </div>
      <div className="flex w-60 flex-col gap-4">
        <Button text="게임 시작" onClick={onClickStart} />
        <Link to="/ranking" className="flex w-full flex-col">
          <Button theme="secondary" text="랭킹 보기" onClick={() => {}} />
        </Link>
      </div>
    </main>
  );
}
