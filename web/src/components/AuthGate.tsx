import { useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { QUERY } from '~/lib/queries';

type AuthGateProps = {
  children: React.ReactElement;
};

export function AuthGate({ children }: AuthGateProps) {
  const { isLoading, isError } = useQuery(QUERY.CURRENT_USER);
  const navigate = useNavigate();

  useEffect(() => {
    if (!isLoading && isError) {
      return navigate('/');
    }
  }, [isLoading, isError, navigate]);

  return <>{children}</>;
}
