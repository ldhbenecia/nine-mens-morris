// 카카오 프로필이 없거나 동의하지 않은 경우 imageUrl 이 null 로 온다
//
// 외부 플레이스홀더 이미지를 쓰면 매번 네트워크 요청이 나가고
// 그 서비스가 죽으면 아바타가 통째로 깨진다. 실제로 via.placeholder.com 이 그랬다
// null 을 그대로 돌려주고 호출부가 닉네임 첫 글자를 그리게 한다
export const avatarOf = (imageUrl: string | null | undefined) =>
  imageUrl ?? null;

// 프로필 사진이 없을 때 쓰는 글자 아바타의 배경색
// 닉네임에서 뽑아 같은 사람은 항상 같은 색이 되게 한다
const COLORS = [
  'bg-slate-500',
  'bg-teal-600',
  'bg-amber-600',
  'bg-rose-500',
  'bg-indigo-500',
  'bg-emerald-600',
];

export const avatarColorOf = (nickname: string) => {
  const sum = [...nickname].reduce((acc, char) => acc + char.charCodeAt(0), 0);
  return COLORS[sum % COLORS.length];
};
