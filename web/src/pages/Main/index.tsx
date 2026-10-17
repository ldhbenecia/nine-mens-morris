import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Button, KakaoButton } from '~/components';
import { QUERY } from '~/lib/queries';
import { UserInfo } from './UserInfo';
import { LoginModal } from './LoginModal';
import { LogoutModal } from './LogoutModal';
import { useLogout } from '~/hooks';
import { TIER_LABEL } from '~/lib/tier';

export function MainPage() {
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [showLogoutModal, setShowLogoutModal] = useState(false);
  const { data: currentUser } = useQuery(QUERY.CURRENT_USER);
  const { mutate } = useLogout();
  const navigate = useNavigate();

  const onClickStart = () =>
    currentUser ? navigate('/rooms') : setShowLoginModal(true);

  const onLogout = () => {
    mutate();
    setShowLogoutModal(false);
  };

  return (
    <main className="flex grow flex-col items-center justify-center gap-12">
      <LoginModal
        visible={showLoginModal}
        onClose={() => setShowLoginModal(false)}
      />

      <LogoutModal
        visible={showLogoutModal}
        visitor={!!currentUser?.visitor}
        onLogout={onLogout}
        onClose={() => setShowLogoutModal(false)}
      />
      {currentUser && (
        <UserInfo
          user={currentUser}
          onShowLogoutModal={() => setShowLogoutModal(true)}
        />
      )}
      <div className="flex flex-col items-center gap-2">
        <h1 className="font-title text-4xl">Nine Men&apos;s Morris</h1>
        <h2 className="text-xl font-light tracking-[0.75rem] text-gray-500">
          나인 멘스 모리스
        </h2>
      </div>
      {/* 게스트는 점수가 움직이지 않아 보여줄 기록이 없음 */}
      {currentUser && !currentUser.visitor && (
        <div className="flex animate-modal-background flex-wrap items-center justify-center gap-x-3 gap-y-1 text-sm text-gray-500">
          <span className="font-semibold text-gray-800">
            {TIER_LABEL[currentUser.tier]} · {currentUser.mmr.toLocaleString()}
          </span>
          <span aria-hidden="true">·</span>
          <span>
            {currentUser.wins}승 {currentUser.losses}패 {currentUser.draws}무
          </span>
          {/* 지금이 최고점이면 같은 숫자를 두 번 보여줄 뿐 */}
          {currentUser.peakMmr > currentUser.mmr && (
            <>
              <span aria-hidden="true">·</span>
              <span>최고 {currentUser.peakMmr.toLocaleString()}</span>
            </>
          )}
          {currentUser.rank !== null && (
            <>
              <span aria-hidden="true">·</span>
              <span>랭킹 {currentUser.rank}위</span>
            </>
          )}
        </div>
      )}
      <div className="flex w-60 flex-col gap-4">
        <Button text="게임 시작" onClick={onClickStart} />
        <Link to="/ranking" className="flex w-full flex-col">
          <Button theme="secondary" text="랭킹 보기" onClick={() => {}} />
        </Link>
        {/* 게스트로 놀다가 로그인할 수 있는 자리
            게임 화면에는 두지 않음. 판 도중에 신원이 바뀌면 그 판을 이어갈 수 없음 */}
        {currentUser?.visitor && (
          <div className="flex flex-col gap-2 border-t border-gray-200 pt-4">
            <span className="text-center text-xs text-gray-500">
              로그인하면 랭킹에 오를 수 있습니다
            </span>
            <KakaoButton />
          </div>
        )}
      </div>
    </main>
  );
}
