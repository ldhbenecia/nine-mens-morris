import Kakao from '~/assets/icons/kakao.svg?react';

export function KakaoButton() {
  return (
    <button
      className="flex w-full cursor-default justify-center gap-2 rounded-lg bg-[#FEE500] p-4 text-lg font-semibold"
      onClick={() =>
        (window.location.href =
          'https://localhost:8080/oauth2/authorization/kakao')
      }
    >
      <Kakao />
      카카오 로그인
    </button>
  );
}
