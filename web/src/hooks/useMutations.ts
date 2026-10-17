import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createRoom,
  createVisitor,
  errorMessageOf,
  joinRoom,
  leaveRoom,
  logout,
} from '~/lib/api';
import { clearToken, setToken } from '~/lib/auth';
import { QUERY } from '~/lib/queries';

// 실패를 화면에 보여줄 책임은 호출부에 있음. 안 넘기면 방이 가득 찼거나 사라졌을 때 클릭이 먹통이 됨
type OnFailure = (message: string) => void;

export const useLogout = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const { mutate } = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearToken();
      queryClient.clear();
      navigate('/');
    },
  });

  return { mutate };
};

// 로그인을 거치지 않고 바로 시작한다
// 발급이 잦으면 서버가 429 로 막으므로 실패 문구를 보여줄 수 있어야 한다
export const useStartAsVisitor = (onFailure?: OnFailure) => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { mutate, isPending } = useMutation({
    mutationFn: createVisitor,
    onSuccess: ({ accessToken }) => {
      setToken(accessToken);
      // 신원이 바뀌었으므로 이전 신원으로 받아둔 응답은 버린다
      queryClient.clear();
      navigate('/rooms');
    },
    onError: (error) => onFailure?.(errorMessageOf(error)),
  });

  return { mutate, isPending };
};

export const useCreateRoom = (onFailure?: OnFailure) => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { mutate } = useMutation({
    mutationFn: (title: string) => createRoom(title),
    onSuccess: ({ roomId }: { roomId: number }) => {
      queryClient.invalidateQueries({ queryKey: QUERY.ROOMS.queryKey });
      navigate(`/game/${roomId}`);
    },
    onError: (error) => onFailure?.(errorMessageOf(error)),
  });

  return { mutate };
};

// 입장을 REST 로 먼저 끝내고 게임 화면으로 감. 소켓으로 입장하면 실패를 응답으로 받을 수 없음
export const useJoinRoom = (onFailure?: OnFailure) => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { mutate } = useMutation({
    mutationFn: (roomId: number) => joinRoom(roomId),
    onSuccess: (_: unknown, roomId: number) => {
      queryClient.invalidateQueries({ queryKey: QUERY.ROOMS.queryKey });
      navigate(`/game/${roomId}`);
    },
    // 방이 가득 찼거나 사라진 경우. 목록도 갱신해 같은 방을 또 누르지 않게 한다
    onError: (error) => {
      queryClient.invalidateQueries({ queryKey: QUERY.ROOMS.queryKey });
      onFailure?.(errorMessageOf(error));
    },
  });

  return { mutate };
};

export const useLeaveRoom = () => {
  const navigate = useNavigate();
  const { mutate } = useMutation({
    mutationFn: (roomId: number) => leaveRoom(roomId),
    onSettled: () => {
      navigate('/rooms');
    },
  });

  return { mutate };
};
