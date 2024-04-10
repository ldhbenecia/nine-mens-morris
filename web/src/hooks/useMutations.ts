import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createRoom, joinRoom } from '~/lib/api';
import { QUERY } from '~/lib/queries';

export const useCreateRoom = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { mutate } = useMutation({
    mutationFn: (roomTitle: string) => createRoom(roomTitle),
    onSuccess: ({ roomId }: { roomId: number }) => {
      queryClient.invalidateQueries({ queryKey: [...QUERY.ROOMS.queryKey] });
      navigate(`/game/${roomId}`);
    },
  });

  return { mutate };
};

export const useJoinRoom = () => {
  const navigate = useNavigate();
  const { mutate } = useMutation({
    mutationFn: (roomId: number) => joinRoom(roomId),
    onSuccess: ({ roomId }: { roomId: number }) => {
      navigate(`/game/${roomId}`);
    },
  });

  return { mutate };
};
