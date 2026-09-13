import axios from 'axios';
import { Rank, Room, RoomDetail, User } from '~/lib/types';

export const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  withCredentials: true,
});

export const logout = async () => {
  const response = await client.post('auth/logout');

  return response.status === 204;
};

export const getCurrentUser = async () => {
  const response = await client.get<User>('users/me');

  return response.data;
};

export const getUserNickname = async (userId: number) => {
  const response = await client.get<{ nickname: string }>(`users/${userId}`);

  return response.data;
};

export const getRanks = async () => {
  const response = await client.get<Rank[]>('rankings');

  return response.data;
};

export const getRooms = async () => {
  const response = await client.get<Room[]>('rooms');

  return response.data;
};

export const createRoom = async (title: string) => {
  const response = await client.post<{ roomId: number; title: string }>(
    'rooms',
    { title }
  );

  return { roomId: response.data.roomId };
};

// 게임 시작 전에도 방장이 누구인지 알아야 함
export const getRoom = async (roomId: number) => {
  const response = await client.get<RoomDetail>(`rooms/${roomId}`);

  return response.data;
};

// 방 입장은 "방에 플레이어를 추가"하는 것이므로 하위 리소스 생성이다
// userId 를 보내지 않는다. 서버가 인증 정보에서 확정한다
export const joinRoom = async (roomId: number) => {
  await client.post(`rooms/${roomId}/players`);
};

export const leaveRoom = async (roomId: number) => {
  await client.delete(`rooms/${roomId}/players/me`);
};
