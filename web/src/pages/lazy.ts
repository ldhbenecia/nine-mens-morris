import { lazy } from 'react';

// 첫 화면(메인)만 바로 싣고 나머지는 들어갈 때 받음
// 게임 화면의 STOMP 까지 첫 접속에 받으면 메인 하나 보려고 번들 전체를 기다림
export const RoomListPage = lazy(() =>
  import('./RoomList').then((m) => ({ default: m.RoomListPage }))
);
export const GamePage = lazy(() =>
  import('./Game').then((m) => ({ default: m.GamePage }))
);
export const RankingPage = lazy(() =>
  import('./Ranking').then((m) => ({ default: m.RankingPage }))
);
export const AuthCallbackPage = lazy(() =>
  import('./AuthCallback').then((m) => ({ default: m.AuthCallbackPage }))
);
