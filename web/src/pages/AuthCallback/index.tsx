import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { Button } from '~/components';
import { setToken } from '~/lib/auth';

const readTokenFromFragment = () =>
  new URLSearchParams(window.location.hash.slice(1)).get('token');

// 로그인 성공 후 서버가 토큰을 프래그먼트에 실어 여기로 보냄
// 프래그먼트는 서버로 전송되지 않아 액세스 로그나 Referer 에 토큰이 남지 않음
export function AuthCallbackPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  // 마운트 때 한 번만 읽음. 아래 effect 가 주소창에서 지운 뒤 다시 읽으면 실패로 보임
  const [token] = useState(readTokenFromFragment);

  useEffect(() => {
    if (!token) return;

    setToken(token);
    // 주소창과 방문 기록에 토큰이 남지 않게 지움
    window.history.replaceState(null, '', window.location.pathname);
    queryClient.clear();
    navigate('/', { replace: true });
  }, [token, navigate, queryClient]);

  if (token) {
    return (
      <main className="flex grow items-center justify-center">
        <span className="animate-pulse text-gray-600">로그인 중...</span>
      </main>
    );
  }

  return (
    <main className="flex grow flex-col items-center justify-center gap-4">
      <span className="font-semibold">로그인에 실패했습니다</span>
      <Button
        text="처음으로"
        onClick={() => navigate('/', { replace: true })}
      />
    </main>
  );
}
