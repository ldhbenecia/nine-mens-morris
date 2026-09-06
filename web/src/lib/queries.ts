import { getCurrentUser, getRanks, getRooms, getUserNickname } from './api';

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
  // 상대가 바뀌면 캐시도 갈려야 하므로 userId 를 키에 넣는다
  // 예전에는 키가 ['userNickname'] 로 고정이라 상대가 바뀌어도 옛 값이 나왔다
  USER_NICKNAME: (userId: number) => ({
    queryKey: ['userNickname', userId],
    queryFn: () => getUserNickname(userId),
  }),
};
