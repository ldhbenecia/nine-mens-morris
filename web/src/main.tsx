import React from 'react';
import ReactDOM from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import '~/styles/index.css';
import { MainPage } from '~/pages/Main';
import { RoomListPage } from './pages/RoomList';
import { GamePage } from './pages/Game';
import { RankingPage } from './pages/Ranking';
import { AuthCallbackPage } from './pages/AuthCallback';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthGate } from './components/AuthGate';

// Pages 하위 경로로 서비스되므로 라우터도 같은 접두사를 알아야 한다
// BASE_URL 은 vite 의 base 설정에서 온다
const router = createBrowserRouter(
  [
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
