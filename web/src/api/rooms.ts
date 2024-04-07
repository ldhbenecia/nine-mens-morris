import { Room } from '~/types';
import { client } from './client';

export const getRooms = async () => {
  const response = await client.get<Room[]>('games');

  return response.data;
};

export const createRoom = async (roomTitle: string) => {
  const response = await client.post<{ id: string; roomTitle: string }>(
    'createGame',
    {
      roomTitle,
    }
  );

  return response.data;
};

export const joinRoom = async (roomId: number) => {
  await client.post<null>('createGame', { roomId });
};
