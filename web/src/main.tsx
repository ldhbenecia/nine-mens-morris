import React from 'react';
import ReactDOM from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import '~/styles/index.css';
import { MainPage } from '~/pages/Main';
import { RoomListPage } from './pages/RoomList';
import { GamePage } from './pages/Game';
import { RankingPage } from './pages/Ranking';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const router = createBrowserRouter([
  {
    path: '/',
    element: <MainPage />,
  },
  {
    path: 'rooms',
    element: <RoomListPage />,
  },
  {
    path: 'game/:roomId',
    element: <GamePage />,
  },
  {
    path: 'ranking',
    element: <RankingPage />,
  },
]);

const queryClient = new QueryClient();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </React.StrictMode>
);
