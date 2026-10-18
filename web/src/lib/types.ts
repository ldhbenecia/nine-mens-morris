export type FirstMoveRule = 'RANDOM' | 'HOST_FIRST' | 'GUEST_FIRST';

export type Tier =
  'UNRANKED' | 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM' | 'DIAMOND' | 'MASTER';

export type Room = {
  roomId: number;
  title: string;
  hostId: number;
  hostNickname: string;
  hostImageUrl: string | null;
  hostRating: number;
  hostVisitor: boolean;
  playerCount: number;
  playing: boolean;
  firstMoveRule: FirstMoveRule;
  rated: boolean;
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
  // 비로그인 사용자는 랭킹에 오르지 않으므로 등수가 없음
  rank: number | null;
  visitor: boolean;
};

// 대전 상대에게 보이는 정보. 내 정보(User)도 같은 필드를 가져 같은 카드로 그림
export type UserProfile = Pick<
  User,
  | 'userId'
  | 'nickname'
  | 'imageUrl'
  | 'mmr'
  | 'tier'
  | 'wins'
  | 'losses'
  | 'draws'
  | 'visitor'
>;

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

// 배치 / 인접 이동 / 자유 이동. 서버가 손에 든 돌과 판 위의 돌 수로 계산해서 내려줌
export type Phase = 'PLACING' | 'MOVING' | 'FLYING';

export type GameStatus = 'WAITING' | 'PLAYING' | 'FINISHED';

export type EndReason =
  | 'STONES_EXHAUSTED'
  | 'NO_LEGAL_MOVE'
  | 'RESIGN'
  | 'DRAW_AGREED'
  | 'THREEFOLD_REPETITION'
  | 'FIFTY_MOVE_RULE'
  | 'ABANDONED'
  | 'TIMEOUT';

// 서버가 흑/백 기준으로 내려줌
// 방장이 항상 흑인 것은 아니라서 host/guest 로는 표현할 수 없음
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
  // 차례인 쪽에 남은 시간. 받은 순간부터 세야 서버와 시계가 어긋나도 맞음. 판이 끝났으면 null
  turnRemainingMillis: number | null;
  // 방이 바뀔 때마다 오르는 번호. 서버 여러 대가 보낸 이벤트가 뒤바뀌어 오면 낡은 쪽을 버리는 기준
  revision: number;
};

export type RoomDetail = {
  roomId: number;
  title: string;
  hostId: number;
  hostNickname: string;
  guestId: number | null;
  guestNickname: string | null;
  firstMoveRule: FirstMoveRule;
  playing: boolean;
  hasGame: boolean; // 끝난 판도 true. 대기실과 판 화면을 고르는 기준
  rated: boolean; // 두 자리가 모두 회원일 때만 true
};

export type RoomEventType =
  | 'LOBBY_CHANGED'
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

// 방 토픽 하나로 흐르는 이벤트. 토픽을 나누지 않음
export type RoomEvent = {
  type: RoomEventType;
  state: GameState | null;
  actorId: number | null;
};

// 판이 아니라 연결에 관한 알림. 지금 붙은 서버가 그 서버의 세션에게만 보냄
export type ServerNotice = {
  type: 'RECONNECT'; // 곧 내려감. 다른 서버에 새 연결을 먼저 붙이고 이 연결을 닫음
};

// 규칙 위반 거절. 방이 아니라 요청자에게만 옴
export type MoveRejected = {
  code: string;
  message: string;
};
