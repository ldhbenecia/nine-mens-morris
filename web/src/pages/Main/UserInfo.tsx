import Close from '~/assets/icons/close.svg?react';
import { Avatar } from '~/components';
import { User } from '~/lib/types';
import { TIER_LABEL } from '~/lib/tier';

type UserInfoProps = {
  user: User;
  onShowLogoutModal: () => void;
};

export function UserInfo({ user, onShowLogoutModal }: UserInfoProps) {
  // 이름과 티어는 정보로만 두고 로그아웃은 별도 버튼. 프로필 전체를 버튼으로 두면 이름만 눌러도 확인창이 뜸
  return (
    user && (
      <div className="fixed top-2 right-2 z-10 flex items-center gap-2 rounded-full bg-white py-1 pr-1 pl-4 shadow-xs">
        <div className="flex flex-col items-end leading-tight">
          <span className="font-semibold">{user.nickname}</span>
          {/* 비로그인 사용자는 MMR 이 움직이지 않는다. 티어를 보여주면 오르는 값처럼 읽힌다 */}
          <span className="text-xs text-gray-500">
            {user.visitor ? '게스트' : `${TIER_LABEL[user.tier]} · ${user.mmr}`}
          </span>
        </div>
        <Avatar nickname={user.nickname} imageUrl={user.imageUrl} />
        <button
          type="button"
          aria-label="로그아웃"
          title="로그아웃"
          onClick={onShowLogoutModal}
          className="flex h-9 w-9 items-center justify-center rounded-full text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-800"
        >
          <Close />
        </button>
      </div>
    )
  );
}
