// 카카오 프로필이 없거나 동의하지 않은 경우 imageUrl 이 null 로 온다
const FALLBACK = 'https://placehold.co/96x96?text=P';

export const avatarOf = (imageUrl: string | null | undefined) =>
  imageUrl ?? FALLBACK;
