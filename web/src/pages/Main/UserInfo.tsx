import Close from '~/assets/icons/close.svg?react';
import { Avatar } from '~/components';
import { User } from '~/lib/types';
import { TIER_LABEL } from '~/lib/tier';

type UserInfoProps = {
  user: User;
  onShowLogoutModal: () => void;
};

export function UserInfo({ user, onShowLogoutModal }: UserInfoProps) {
  // 예전엔 프로필 전체가 로그아웃 버튼이라 이름만 눌러도 확인창이 떴음
  // 이름과 티어는 정보로만 두고 로그아웃은 별도 버튼으로 분리
  return (
    user && (
      <div className="fixed right-2 top-2 z-10 flex items-center gap-2 rounded-full bg-white py-1 pl-4 pr-1 shadow-sm">
        <div className="flex flex-col items-end leading-tight">
          <span className="font-semibold">{user.nickname}</span>
          <span className="text-xs text-gray-500">
            {TIER_LABEL[user.tier]} · {user.mmr}
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
