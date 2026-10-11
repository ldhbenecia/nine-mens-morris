import {
  getCurrentUser,
  getRanks,
  getRoom,
  getRooms,
  getUserProfile,
} from './api';
import { getToken } from './auth';

export const QUERY = {
  ROOMS: {
    queryKey: ['rooms'],
    queryFn: getRooms,
  },
  // 토큰이 없으면 묻지 않음. 물으면 비로그인 접속마다 401 이 콘솔에 남음
  // 함수로 둬야 게스트 발급·로그인 직후 다시 평가됨
  CURRENT_USER: {
    queryKey: ['currentUser'],
    queryFn: getCurrentUser,
    enabled: () => !!getToken(),
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
  USER_PROFILE: (userId: number) => ({
    queryKey: ['userProfile', userId],
    queryFn: () => getUserProfile(userId),
  }),
};
