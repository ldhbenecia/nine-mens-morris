import { createRoom, joinRoom } from '~/api';

export const MUTATIONS = {
  CREATE_ROOM: {
    mutationFn: (roomTitle: string) => createRoom(roomTitle),
    // onSuccess: 방 생성 후 해당 방으로 이동
  },
  JOIN_ROOM: {
    mutationFn: (roomId: number) => joinRoom(roomId),
    // onSuccess: 방 참가 후 해당 방으로 이동
  },
};
