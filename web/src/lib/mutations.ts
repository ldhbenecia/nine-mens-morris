import { redirect } from 'react-router-dom';
import { createRoom, joinRoom } from './api';

export const MUTATIONS = {
  CREATE_ROOM: {
    mutationFn: (roomTitle: string) => createRoom(roomTitle),
    onSuccess: ({ id }: { id: number }) => redirect(`game/${id}`),
  },
  JOIN_ROOM: {
    mutationFn: (roomId: number) => joinRoom(roomId),
    onSuccess: (roomId: number) => redirect(`game/${roomId}`),
  },
};
