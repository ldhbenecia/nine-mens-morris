import axios from 'axios';
import { Rank, Room, User } from '~/lib/types';

export const client = axios.create({
  baseURL: 'http://localhost:8080/api/',
});

export const getCurrentUser = async () => {
  const response = await client.get<User>('user');

  return response.data;
};

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
  await client.post<null>(`games/${roomId}`);
};

export const getRanks = async () => {
  const response = await client.get<Rank[]>('rank');

  return response.data;
};
