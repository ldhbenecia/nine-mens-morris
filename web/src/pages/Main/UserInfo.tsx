import Close from '~/assets/icons/close.svg?react';
import { User } from '~/lib/types';

type UserInfoProps = {
  user: User | null;
  onShowLogoutModal: () => void;
};

export function UserInfo({ user, onShowLogoutModal }: UserInfoProps) {
  return (
    user && (
      <>
        <div
          className="fixed right-2 top-2 z-10 flex items-center gap-2 rounded-full bg-white p-1 pl-4 font-semibold opacity-0 transition-opacity hover:opacity-100"
          onClick={onShowLogoutModal}
        >
          로그아웃
          <div className="flex h-10 w-10 items-center justify-center overflow-hidden rounded-full bg-gray-800 active:bg-black">
            <Close />
          </div>
        </div>
        <div className="fixed right-2 top-2 flex items-center gap-2 rounded-full bg-white p-1 pl-4">
          {user.nickname}
          <div className="h-10 w-10 overflow-hidden rounded-full">
            <img src={user.imageUrl} alt={user.nickname} />
          </div>
        </div>
      </>
    )
  );
}
