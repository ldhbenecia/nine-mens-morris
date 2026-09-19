import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createRoom,
  errorMessageOf,
  joinRoom,
  leaveRoom,
  logout,
} from '~/lib/api';
import { clearToken } from '~/lib/auth';
import { QUERY } from '~/lib/queries';

// 실패를 화면에 보여줄 책임은 호출부에 있다
// 예전에는 onError 가 아예 없어서 방이 가득 찼거나 사라진 경우가 먹통 클릭이었다
type OnFailure = (message: string) => void;

export const useLogout = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const { mutate } = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearToken();
      queryClient.clear();
      navigate('/');
    },
  });

  return { mutate };
};

export const useCreateRoom = (onFailure?: OnFailure) => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { mutate } = useMutation({
    mutationFn: (title: string) => createRoom(title),
    onSuccess: ({ roomId }: { roomId: number }) => {
      queryClient.invalidateQueries({ queryKey: QUERY.ROOMS.queryKey });
      navigate(`/game/${roomId}`);
    },
    onError: (error) => onFailure?.(errorMessageOf(error)),
  });

  return { mutate };
};

// 입장을 REST 로 먼저 끝내고 게임 화면으로 간다
// 예전에는 소켓이 연결된 뒤 /app/joinGame 으로 입장해서
// 입장 실패를 화면에서 알 방법이 없었다
export const useJoinRoom = (onFailure?: OnFailure) => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { mutate } = useMutation({
    mutationFn: (roomId: number) => joinRoom(roomId),
    onSuccess: (_: unknown, roomId: number) => {
      queryClient.invalidateQueries({ queryKey: QUERY.ROOMS.queryKey });
      navigate(`/game/${roomId}`);
    },
    // 방이 가득 찼거나 사라진 경우. 목록도 갱신해 같은 방을 또 누르지 않게 한다
    onError: (error) => {
      queryClient.invalidateQueries({ queryKey: QUERY.ROOMS.queryKey });
      onFailure?.(errorMessageOf(error));
    },
  });

  return { mutate };
};

export const useLeaveRoom = () => {
  const navigate = useNavigate();
  const { mutate } = useMutation({
    mutationFn: (roomId: number) => leaveRoom(roomId),
    onSettled: () => {
      navigate('/rooms');
    },
  });

  return { mutate };
};
