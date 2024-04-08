export type Room = {
  roomId: number;
  roomTitle: string;
  host: string;
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
  nickname: 'string';
  score: number;
};

export type StoneType = 'EMPTY' | 'WHITE' | 'BLACK';

export type PointType = {
  top: number;
  left: number;
  stone: StoneType;
  selected: boolean;
};
