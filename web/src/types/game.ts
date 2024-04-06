export type StoneType = 'EMPTY' | 'WHITE' | 'BLACK';
export type PointType = {
  top: number;
  left: number;
  stone: StoneType;
  selected: boolean;
};
