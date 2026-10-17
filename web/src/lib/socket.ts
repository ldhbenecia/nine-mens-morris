import { Client } from '@stomp/stompjs';
import { authHeaders } from './auth';

// STOMP 목적지. 서버의 StompDestinations 와 같이 바꿔야 함
export const DESTINATION = {
  LOBBY: '/topic/lobby', // 방 목록이 바뀌었다는 신호. 목록은 REST 로 다시 받음
  ERRORS: '/user/queue/errors', // 나에게만 오는 거절 사유
  SYNC: '/user/queue/sync', // 나에게만 오는 판 전체 상태
  room: (roomId: number) => `/topic/rooms/${roomId}`,
  roomAction: (roomId: number, action: RoomAction) =>
    `/app/rooms/${roomId}/${action}`,
};

export type RoomAction =
  | 'settings'
  | 'start'
  | 'place'
  | 'move'
  | 'remove'
  | 'resign'
  | 'draw-offer'
  | 'draw-accept'
  | 'draw-decline'
  | 'sync';

// 토큰은 CONNECT 프레임 헤더로 보냄
// 핸드셰이크는 브라우저가 보내는 HTTP 요청이라 헤더를 붙일 수 없고, 쿠키는 cross-site 라 못 씀
// beforeConnect 에서 채워야 재연결 때도 최신 토큰이 실림
//
// 하트비트는 기본값(10초)으로 둠. 서버도 10초라 더 짧게 요청해도 협상은 큰 값으로 정해짐
export const createSocketClient = () => {
  const client = new Client({
    brokerURL: import.meta.env.VITE_SOCKET_URL,
    reconnectDelay: 5000,
    beforeConnect: () => {
      client.connectHeaders = authHeaders();
    },
  });
  return client;
};
