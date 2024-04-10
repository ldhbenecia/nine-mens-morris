import axios from 'axios';
import { Rank, Room, User } from '~/lib/types';

export const client = axios.create({
  baseURL: 'http://localhost:8080/api/',
  withCredentials: true,
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
  const response = await client.post<{ id: number }>('createGame', {
    roomTitle,
  });

  return response.data;
};

export const joinRoom = async (roomId: number) => {
  const response = await client.post<null>(`joinGame/${roomId}`);

  return response.status === 201 ? roomId : 0;
};

export const getRanks = async () => {
  const response = await client.get<Rank[]>('rank');

  return response.data;
};
