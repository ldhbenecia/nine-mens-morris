import React from 'react';
import ReactDOM from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import '~/styles/index.css';
import { MainPage } from '~/pages/Main';
import { RoomListPage } from './pages/RoomList';
import { GamePage } from './pages/Game';

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
    path: 'game',
    element: <GamePage />,
  },
]);

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>
);
