export type Room = {
  roomId: number;
  roomTitle: string;
  playerCount: number;
  host: string;
  hostImageUrl: string;
  hostScore: number;
};

export type User = {
  userId: number;
  email: string;
  nickname: string;
  imageUrl: string;
  role: 'USER' | 'ADMIN';
  score: number;
};

export type Rank = {
  nickname: string;
  imageUrl: string;
  score: number;
};

export type StoneType = 'EMPTY' | 'WHITE' | 'BLACK';

export type PointType = {
  top: number;
  left: number;
  stone: StoneType;
  selected: boolean;
};

export type GameState = {
  roomId: number;
  board: StoneType[];
  hostId: number;
  guestId: number;
  currentTurn: number | null;
  addable: [number, number];
  total: [number, number];
  status: 'WAITING' | 'PLAYING' | 'FINISHED';
  phase: 1 | 2;
  winner: number | null;
  loser: number | null;
};
