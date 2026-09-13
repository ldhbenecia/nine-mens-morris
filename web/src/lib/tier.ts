import { Tier } from '~/lib/types';

// 서버는 등급을 영어로 내려준다. 화면 표기는 여기서 매핑한다
export const TIER_LABEL: Record<Tier, string> = {
  UNRANKED: '언랭크',
  BRONZE: '브론즈',
  SILVER: '실버',
  GOLD: '골드',
  PLATINUM: '플래티넘',
  DIAMOND: '다이아몬드',
  MASTER: '마스터',
};

export const TIER_STYLE: Record<Tier, string> = {
  UNRANKED: 'text-gray-400',
  BRONZE: 'text-amber-700',
  SILVER: 'text-slate-500',
  GOLD: 'text-amber-500',
  PLATINUM: 'text-teal-500',
  DIAMOND: 'text-sky-500',
  MASTER: 'text-purple-600',
};
