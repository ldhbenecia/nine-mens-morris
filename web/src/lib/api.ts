import axios from 'axios';
import { Rank, Room, User } from '~/lib/types';

export const client = axios.create({
  baseURL:
    import.meta.env.MODE === 'development'
      ? 'http://localhost:8080/api'
      : 'https://api.ninemensmorris.site:8080/api',
  withCredentials: true,
});

export const logout = async () => {
  const response = await client.post('auth/logout');

  return response.status === 200;
};

export const getCurrentUser = async () => {
  const response = await client.get<User>('user');

  if (response.status === 200) {
    return response.data;
  }

  throw new Error('로그아웃된 사용자입니다.');
};

export const getRanks = async () => {
  const response = await client.get<Rank[]>('rank');

  return response.data;
};

export const getRooms = async () => {
  const response = await client.get<Room[]>('games');

  return response.data;
};

export const createRoom = async (roomTitle: string) => {
  const response = await client.post<{
    roomId: number;
    roomTitle: number;
    host: string;
  }>('createGame', {
    roomTitle,
  });

  return { roomId: response.status === 201 ? response.data.roomId : -1 };
};

export const leaveRoom = async (roomId: number) => {
  const response = await client.post(`leaveGame/${roomId}`);

  return response.status === 200;
};
