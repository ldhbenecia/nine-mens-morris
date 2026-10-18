import { Client, IPublishParams, ReconnectionTimeMode } from '@stomp/stompjs';
import { authHeaders } from './auth';
import { ServerNotice } from './types';

// STOMP 목적지. 서버의 StompDestinations 와 같이 바꿔야 함
export const DESTINATION = {
  LOBBY: '/topic/lobby', // 방 목록이 바뀌었다는 신호. 목록은 REST 로 다시 받음
  SYSTEM: '/topic/system', // 지금 붙은 서버가 보내는 연결 관리 신호
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

export type Publisher = {
  publish: (params: IPublishParams) => boolean; // 연결이 없어 못 보냈으면 false
};

// 토큰은 CONNECT 프레임 헤더로 보냄
// 핸드셰이크는 브라우저가 보내는 HTTP 요청이라 헤더를 붙일 수 없고, 쿠키는 cross-site 라 못 씀
// beforeConnect 에서 채워야 재연결 때도 최신 토큰이 실림
//
// 하트비트는 기본값(10초)으로 둠. 서버도 10초라 더 짧게 요청해도 협상은 큰 값으로 정해짐
// 재연결은 0.5초부터 두 배씩 5초까지. 서버 교체처럼 곧 붙을 끊김은 바로 이어지고, 길어지면 덜 두드림
const createSocketClient = () => {
  const client = new Client({
    brokerURL: import.meta.env.VITE_SOCKET_URL,
    reconnectDelay: 500,
    reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
    maxReconnectDelay: 5000,
    beforeConnect: () => {
      client.connectHeaders = authHeaders();
    },
  });
  return client;
};

// 이 시간 안에 다시 붙으면 끊긴 것을 화면에 알리지 않음
const LOST_AFTER_MILLIS = 3000;

type SocketHandlers = {
  onConnect: (client: Client) => void; // 새 연결마다 부름. 구독과 sync 를 여기서
  onLost?: () => void;
  onRestored?: () => void;
  onError?: (message: string) => void;
};

// 끊김이 화면에 보이지 않게 연결을 관리함
//
// 서버가 내려가기 전에 RECONNECT 를 보내면 다른 서버에 새 연결을 먼저 붙이고, 구독과 sync 를 마친 뒤 옛 연결을 닫음
// 판은 서버가 아니라 Redis 에 있어 두 연결이 잠깐 겹쳐도 되고, 옮기는 사이 옛 연결로 보낸 수도 처리됨
// 예고 없이 끊기면(서버 장애) 조용히 다시 붙고, LOST_AFTER_MILLIS 넘게 못 붙을 때만 onLost 를 부름
export class ManagedSocket implements Publisher {
  private current: Client | null = null;
  private next: Client | null = null;
  private handlers: SocketHandlers | null = null;
  private lostTimer = 0;
  private lost = false;

  start(handlers: SocketHandlers) {
    this.handlers = handlers;
    this.current = this.open();
  }

  stop() {
    window.clearTimeout(this.lostTimer);
    this.lostTimer = 0;
    this.lost = false;
    this.handlers = null;
    [this.current, this.next].forEach((client) => client && retire(client));
    this.current = null;
    this.next = null;
  }

  publish(params: IPublishParams) {
    if (!this.current?.connected) return false;
    this.current.publish(params);
    return true;
  }

  private open() {
    const client = createSocketClient();
    client.onConnect = () => {
      client.subscribe(DESTINATION.SYSTEM, (message) => {
        const notice = JSON.parse(message.body) as ServerNotice;
        if (notice.type === 'RECONNECT') this.handOff();
      });
      this.handlers?.onConnect(client);
      // 새 연결이 구독과 sync 를 마친 뒤에야 옛 연결을 닫음. 먼저 닫으면 그 사이 이벤트를 놓침
      if (client === this.next) {
        const previous = this.current;
        this.current = client;
        this.next = null;
        if (previous) retire(previous);
      }
      if (client === this.current) this.restored();
    };
    client.onWebSocketClose = () => {
      if (client === this.current) this.dropped();
    };
    client.onStompError = (frame) =>
      this.handlers?.onError?.(
        frame.headers.message ?? '서버와 통신에 실패했습니다.'
      );
    client.activate();
    return client;
  }

  private handOff() {
    if (this.next || !this.handlers) return;
    this.next = this.open();
  }

  private dropped() {
    if (this.lostTimer || this.lost) return;
    this.lostTimer = window.setTimeout(() => {
      this.lostTimer = 0;
      this.lost = true;
      this.handlers?.onLost?.();
    }, LOST_AFTER_MILLIS);
  }

  private restored() {
    window.clearTimeout(this.lostTimer);
    this.lostTimer = 0;
    if (!this.lost) return;
    this.lost = false;
    this.handlers?.onRestored?.();
  }
}

// 의도적으로 닫는 연결은 끊김으로 세지 않게 먼저 떼어냄
const retire = (client: Client) => {
  client.onWebSocketClose = () => {};
  client.onConnect = () => {};
  client.deactivate();
};
