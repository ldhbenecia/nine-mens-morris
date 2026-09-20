type RatedBadgeProps = {
  rated: boolean;
};

// 게스트가 한 명이라도 끼면 서버가 레이팅을 움직이지 않는다
// 판이 시작된 뒤에 알면 늦으므로 목록과 대기실 양쪽에 같은 문구로 붙인다
export function RatedBadge({ rated }: RatedBadgeProps) {
  return (
    <span
      className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-semibold ${
        rated ? 'bg-gray-800 text-white' : 'bg-gray-200 text-gray-600'
      }`}
    >
      {rated ? '랭크전' : '일반전'}
    </span>
  );
}
