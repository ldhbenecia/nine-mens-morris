import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { Button } from '~/components';
import { setToken } from '~/lib/auth';

// 로그인 성공 후 서버가 토큰을 프래그먼트에 실어 여기로 보낸다
// 프래그먼트는 서버로 전송되지 않아 액세스 로그나 Referer 에 토큰이 남지 않는다
export function AuthCallbackPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const token = new URLSearchParams(window.location.hash.slice(1)).get(
      'token'
    );
    if (!token) {
      setFailed(true);
      return;
    }

    setToken(token);
    // 주소창과 방문 기록에 토큰이 남지 않게 지운다
    window.history.replaceState(null, '', window.location.pathname);
    queryClient.clear();
    navigate('/', { replace: true });
  }, [navigate, queryClient]);

  if (!failed) {
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
