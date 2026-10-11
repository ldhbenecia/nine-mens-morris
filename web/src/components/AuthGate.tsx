import { useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { QUERY } from '~/lib/queries';

type AuthGateProps = {
  children: React.ReactElement;
};

// 신원이 확인될 때까지 자식을 그리지 않음
// 먼저 그리면 게임 화면이 토큰 없이 소켓부터 열어 거절당하고 에러 창이 한 번 번쩍임
export function AuthGate({ children }: AuthGateProps) {
  const {
    data: currentUser,
    isLoading,
    isError,
  } = useQuery(QUERY.CURRENT_USER);
  const navigate = useNavigate();

  useEffect(() => {
    if (!isLoading && isError) {
      navigate('/', { replace: true });
    }
  }, [isLoading, isError, navigate]);

  if (!currentUser) {
    return (
      <main className="flex grow items-center justify-center">
        <span className="animate-pulse text-gray-500">확인 중...</span>
      </main>
    );
  }
  return children;
}
