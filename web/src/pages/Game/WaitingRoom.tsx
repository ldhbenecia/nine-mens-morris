import Logout from '~/assets/icons/logout.svg?react';
import { Button } from '~/components';
import { FirstMoveRule, RoomDetail } from '~/lib/types';

type WaitingRoomProps = {
  room: RoomDetail | undefined;
  isHost: boolean;
  onChangeFirstMoveRule: (rule: FirstMoveRule) => void;
  onStart: () => void;
  onLeave: () => void;
};

const RULE_LABEL: Record<FirstMoveRule, string> = {
  RANDOM: '랜덤',
  HOST_FIRST: '방장 선공',
  GUEST_FIRST: '참가자 선공',
};

const RULES: FirstMoveRule[] = ['RANDOM', 'HOST_FIRST', 'GUEST_FIRST'];

// 예전에는 두 번째 사람이 들어오는 순간 자동으로 시작해서 대기실이 없었다
// 선공을 고를 수 있게 되면서 "모였지만 아직 시작 전" 이라는 단계가 생겼다
export function WaitingRoom({
  room,
  isHost,
  onChangeFirstMoveRule,
  onStart,
  onLeave,
}: WaitingRoomProps) {
  const opponentReady = !!room?.guestId;

  return (
    <div className="flex grow flex-col items-center justify-center gap-8">
      <div className="flex flex-col items-center gap-1">
        <h1 className="text-2xl font-semibold">{room?.title ?? '　'}</h1>
        <span className="text-sm text-gray-500">
          {isHost ? '내가 방장입니다' : `방장: ${room?.hostNickname ?? ''}`}
        </span>
      </div>

      <div className="flex items-center gap-4">
        <PlayerSlot name={room?.hostNickname} role="방장" />
        <span className="text-xl font-semibold text-gray-400">VS</span>
        <PlayerSlot name={room?.guestNickname} role="참가자" />
      </div>

      <div className="flex w-80 flex-col items-center gap-2">
        <span className="text-sm font-semibold text-gray-600">선공</span>
        <div className="flex w-full gap-2">
          {RULES.map((rule) => {
            const selected = room?.firstMoveRule === rule;
            return (
              <button
                key={rule}
                type="button"
                disabled={!isHost}
                onClick={() => onChangeFirstMoveRule(rule)}
                className={`grow rounded-lg border px-2 py-2 text-sm transition-colors
                  ${selected ? 'border-gray-800 bg-gray-800 font-semibold text-white' : 'border-gray-300 bg-white'}
                  ${isHost ? 'cursor-pointer hover:border-gray-500' : 'cursor-default opacity-70'}`}
              >
                {RULE_LABEL[rule]}
              </button>
            );
          })}
        </div>
        {!isHost && (
          <span className="text-xs text-gray-500">방장이 선공을 정합니다.</span>
        )}
      </div>

      <div className="flex w-72 flex-col gap-2">
        {isHost ? (
          <Button
            text={opponentReady ? '게임 시작' : '상대를 기다리는 중...'}
            fullWidth
            disabled={!opponentReady}
            onClick={onStart}
          />
        ) : (
          <span className="animate-pulse py-3 text-center text-gray-600">
            방장이 시작하기를 기다리는 중...
          </span>
        )}
        <Button
          text="나가기"
          theme="secondary"
          fullWidth
          icon={<Logout />}
          onClick={onLeave}
        />
      </div>
    </div>
  );
}

function PlayerSlot({ name, role }: { name?: string | null; role: string }) {
  return (
    <div
      className={`flex h-24 w-40 flex-col items-center justify-center gap-1 rounded-xl border
        ${name ? 'border-gray-300 bg-gray-50' : 'border-dashed border-gray-300 bg-white'}`}
    >
      <span className="text-xs text-gray-500">{role}</span>
      <span className={`font-semibold ${name ? '' : 'text-gray-400'}`}>
        {name ?? '비어 있음'}
      </span>
    </div>
  );
}
