import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createRoom, joinRoom, leaveRoom, logout } from '~/lib/api';
import { QUERY } from '~/lib/queries';

export const useLogout = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { mutate } = useMutation({
    mutationFn: logout,
    onSuccess: () => {
      queryClient.removeQueries({
        queryKey: QUERY.CURRENT_USER.queryKey,
      });
      navigate('/');
    },
  });

  return { mutate };
};

export const useCreateRoom = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { mutate } = useMutation({
    mutationFn: (title: string) => createRoom(title),
    onSuccess: ({ roomId }: { roomId: number }) => {
      queryClient.invalidateQueries({ queryKey: QUERY.ROOMS.queryKey });
      navigate(`/game/${roomId}`);
    },
  });

  return { mutate };
};

// 입장을 REST 로 먼저 끝내고 게임 화면으로 간다
// 예전에는 소켓이 연결된 뒤 /app/joinGame 으로 입장해서
// 입장 실패를 화면에서 알 방법이 없었다
export const useJoinRoom = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { mutate } = useMutation({
    mutationFn: (roomId: number) => joinRoom(roomId),
    onSuccess: (_, roomId) => {
      queryClient.invalidateQueries({ queryKey: QUERY.ROOMS.queryKey });
      navigate(`/game/${roomId}`);
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
