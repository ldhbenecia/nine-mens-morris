import { avatarColorOf, avatarOf } from '~/lib/avatar';

type AvatarProps = {
  nickname: string;
  imageUrl: string | null | undefined;
  size?: 'sm' | 'md' | 'lg';
};

const SIZE = {
  sm: 'h-9 w-9 text-sm',
  md: 'h-10 w-10',
  lg: 'h-12 w-12 text-lg',
};

// 프로필 사진이 없으면 닉네임 첫 글자를 그린다
// 외부 플레이스홀더 이미지를 쓰면 매번 네트워크 요청이 나가고
// 그 서비스가 죽으면 아바타가 통째로 깨진다
export function Avatar({ nickname, imageUrl, size = 'md' }: AvatarProps) {
  const source = avatarOf(imageUrl);

  if (!source) {
    return (
      <div
        className={`flex shrink-0 items-center justify-center rounded-full font-semibold text-white ${SIZE[size]} ${avatarColorOf(nickname)}`}
      >
        {nickname.charAt(0)}
      </div>
    );
  }

  return (
    <img
      src={source}
      alt={nickname}
      className={`shrink-0 rounded-full object-cover ${SIZE[size]}`}
    />
  );
}
