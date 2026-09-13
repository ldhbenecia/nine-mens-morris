import {
  getCurrentUser,
  getRanks,
  getRoom,
  getRooms,
  getUserNickname,
} from './api';

export const QUERY = {
  ROOMS: {
    queryKey: ['rooms'],
    queryFn: getRooms,
  },
  CURRENT_USER: {
    queryKey: ['currentUser'],
    queryFn: getCurrentUser,
    retry: false,
  },
  RANKS: {
    queryKey: ['ranks'],
    queryFn: getRanks,
  },
  // 방이 사라졌으면 재시도해도 계속 404 라 바로 포기시킴
  ROOM: (roomId: number) => ({
    queryKey: ['room', roomId],
    queryFn: () => getRoom(roomId),
    retry: false,
  }),
  // 상대가 바뀌면 캐시도 갈려야 하므로 userId 를 키에 넣음
  // 예전에는 키가 ['userNickname'] 로 고정이라 상대가 바뀌어도 옛 값이 나왔음
  USER_NICKNAME: (userId: number) => ({
    queryKey: ['userNickname', userId],
    queryFn: () => getUserNickname(userId),
  }),
};
