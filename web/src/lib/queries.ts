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
  USER_NICKNAME: {
    queryKey: ['userNickname'],
    queryFn: getUserNickname,
  },
};
