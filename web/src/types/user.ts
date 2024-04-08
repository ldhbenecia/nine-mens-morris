export type User = {
  username: string;
  rank: number;
  profileImageSrc: string;
};

export type CurrentUser = {
  userId: number;
  email: string;
  nickname: string;
  imageUrl: string;
  role: string;
  score: number;
};
