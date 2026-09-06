export type FirstMoveRule = 'RANDOM' | 'HOST_FIRST' | 'GUEST_FIRST';

export type Tier =
  | 'UNRANKED'
  | 'BRONZE'
  | 'SILVER'
  | 'GOLD'
  | 'PLATINUM'
  | 'DIAMOND'
  | 'MASTER';

export type Room = {
  roomId: number;
  title: string;
  hostId: number;
  hostNickname: string;
  hostImageUrl: string | null;
  hostRating: number;
  playerCount: number;
  playing: boolean;
  firstMoveRule: FirstMoveRule;
};

export type User = {
  userId: number;
  nickname: string;
  imageUrl: string | null;
  mmr: number;
  peakMmr: number;
  tier: Tier;
  wins: number;
  losses: number;
  draws: number;
  rank: number | null;
};

export type Rank = {
  rank: number;
  userId: number;
  nickname: string;
  imageUrl: string | null;
  mmr: number;
  tier: Tier;
  wins: number;
  losses: number;
  draws: number;
};

export type StoneType = 'EMPTY' | 'WHITE' | 'BLACK';

export type PointType = {
  top: number;
  left: number;
  stone: StoneType;
};

// 배치 / 인접 이동 / 자유 이동. 서버가 손에 든 돌과 판 위의 돌 수로 계산해서 내려준다
export type Phase = 'PLACING' | 'MOVING' | 'FLYING';

export type GameStatus = 'WAITING' | 'PLAYING' | 'FINISHED';

export type EndReason =
  | 'STONES_EXHAUSTED'
  | 'NO_LEGAL_MOVE'
  | 'RESIGN'
  | 'DRAW_AGREED'
  | 'THREEFOLD_REPETITION'
  | 'FIFTY_MOVE_RULE';

// 서버가 흑/백 기준으로 내려준다
// 방장이 항상 흑이라는 전제가 사라져서 host/guest 로는 표현할 수 없다
export type GameState = {
  board: StoneType[];
  blackId: number;
  whiteId: number;
  currentTurnId: number;
  blackInHand: number;
  whiteInHand: number;
  blackOnBoard: number;
  whiteOnBoard: number;
  blackPhase: Phase;
  whitePhase: Phase;
  awaitingRemoval: boolean;
  status: GameStatus;
  winnerId: number | null;
  loserId: number | null;
  endReason: EndReason | null;
};

export type RoomEventType =
  | 'PLAYER_JOINED'
  | 'PLAYER_LEFT'
  | 'SETTINGS_CHANGED'
  | 'STARTED'
  | 'STATE_CHANGED'
  | 'FINISHED'
  | 'DRAW_OFFERED'
  | 'DRAW_DECLINED'
  | 'SNAPSHOT'
  | 'OPPONENT_DISCONNECTED';

// 방 토픽 하나로 흐르는 이벤트. 예전처럼 토픽이 둘로 나뉘어 있지 않다
export type RoomEvent = {
  type: RoomEventType;
  state: GameState | null;
  actorId: number | null;
  message: string | null;
};

// 규칙 위반 거절. 방이 아니라 요청자에게만 온다
export type MoveRejected = {
  code: string;
  message: string;
};
