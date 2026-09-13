import Kakao from '~/assets/icons/kakao.svg?react';

export function KakaoButton() {
  return (
    <button
      className="flex w-full cursor-pointer justify-center gap-2 rounded-lg bg-[#FEE500] p-3 font-semibold"
      onClick={() => (window.location.href = import.meta.env.VITE_KAKAO_URL)}
    >
      <Kakao />
      카카오 로그인
    </button>
  );
}
