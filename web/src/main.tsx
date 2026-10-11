import React from 'react';
import ReactDOM from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import '~/styles/index.css';
import { MainPage } from '~/pages/Main';
import { RoomListPage } from './pages/RoomList';
import { GamePage } from './pages/Game';
import { RankingPage } from './pages/Ranking';
import { AuthCallbackPage } from './pages/AuthCallback';
import { ErrorPage, NotFoundPage } from './pages/Error';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthGate } from './components/AuthGate';

// Pages 하위 경로로 서비스되므로 라우터도 같은 접두사를 알아야 함
// BASE_URL 은 vite 의 base 설정에서 옴
//
// 모든 경로를 경로 없는 부모 아래에 두어 errorElement 를 한 곳에서 걸음
// 없으면 렌더 중 예외와 없는 주소에 라우터의 개발자용 기본 화면이 그대로 노출됨
const router = createBrowserRouter(
  [
    {
      errorElement: <ErrorPage />,
      children: [
        {
          path: '/',
          element: <MainPage />,
        },
        {
          path: 'rooms',
          element: (
            <AuthGate>
              <RoomListPage />
            </AuthGate>
          ),
        },
        {
          path: 'game/:roomId',
          element: (
            <AuthGate>
              <GamePage />
            </AuthGate>
          ),
        },
        {
          path: 'ranking',
          element: <RankingPage />,
        },
        {
          path: 'auth/callback',
          element: <AuthCallbackPage />,
        },
        {
          path: '*',
          element: <NotFoundPage />,
        },
      ],
    },
  ],
  { basename: import.meta.env.BASE_URL }
);

const queryClient = new QueryClient();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </React.StrictMode>
);
