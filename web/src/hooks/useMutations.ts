import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createRoom, leaveRoom, logout } from '~/lib/api';
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
    mutationFn: (roomTitle: string) => createRoom(roomTitle),
    onSuccess: ({ roomId }: { roomId: number }) => {
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
