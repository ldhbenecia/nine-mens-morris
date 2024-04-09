import Play from '~/assets/icons/play.svg?react';
import Flag from '~/assets/icons/flag.svg?react';
import Help from '~/assets/icons/help.svg?react';
import { Button } from '~/components/Button';

type StatusProps = {
  isCurrentUser: boolean;
  isTurn: boolean;
  color: 'WHITE' | 'BLACK';
  remaining: number;
  onWithdraw?: () => void;
};

export function Status({
  isCurrentUser,
  isTurn,
  color,
  remaining,
  onWithdraw,
}: StatusProps) {
  return (
    <div className={`flex ${isCurrentUser ? '-translate-x-3 ' : ''}`}>
      <div className="gap-2">
        {isTurn && <Play className="animate-pulse" />}
      </div>
      <div className="flex w-48 flex-col gap-1">
        <div className="flex items-center justify-between">
          <span className="text-lg font-semibold">
            {isCurrentUser ? '나' : '상대'}
          </span>
          <div
            className={`rounded-2xl border px-1.5 text-sm ${isCurrentUser ? 'border-gray-300 bg-gray-50' : 'border-gray-800 bg-gray-800 text-white'}`}
          >
            {color === 'WHITE' ? '백돌' : '흑돌'}
          </div>
        </div>
        <div className="flex justify-between">
          <div className="flex items-center gap-0.5 font-semibold">
            {[...Array(remaining)].map((_, idx) => (
              <div
                key={idx}
                className={`h-3.5 w-3.5 rounded-full border ${color === 'WHITE' ? 'border-gray-500 bg-gray-50' : 'border-gray-800 bg-gray-800'}`}
              />
            ))}
            {[...Array(9 - remaining)].map((_, idx) => (
              <div
                key={idx}
                className={`h-3.5 w-3.5 rounded-full bg-gray-200`}
              />
            ))}
          </div>
          <span className="px-1 text-sm font-semibold">{remaining} / 9</span>
        </div>
        {isCurrentUser && (
          <div className="mt-2 flex gap-2">
            <Button
              text="기권하기"
              icon={<Flag width={18} height={18} />}
              slim
              small
              fullWidth
              onClick={onWithdraw}
            />
            <Button
              theme="secondary"
              text="도움말"
              icon={<Help width={18} height={18} />}
              slim
              small
              fullWidth
            />
          </div>
        )}
      </div>
    </div>
  );
}
