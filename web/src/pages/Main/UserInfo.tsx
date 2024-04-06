import { User } from '~/types';

type UserInfoProps = {
  user: User | null;
};

export function UserInfo({ user }: UserInfoProps) {
  return (
    user && (
      <div className="fixed right-2 top-2 flex items-center gap-2 rounded-full border border-gray-300 bg-gray-50 p-1 pl-4 hover:bg-white active:bg-gray-100">
        {user.username}
        <div className="h-10 w-10 overflow-hidden rounded-full">
          <img src={user.profileImageSrc} alt={user.username} />
        </div>
      </div>
    )
  );
}
