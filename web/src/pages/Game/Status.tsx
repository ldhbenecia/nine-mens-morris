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
    <div className="flex">
      <div className="pt-0.5">
        {isTurn && <Play className="animate-pulse" />}
      </div>
      <div className="flex w-64 flex-col gap-3">
        <div className="flex items-center justify-between">
          <span className="text-xl font-semibold">
            {isCurrentUser ? '나' : '상대'}
          </span>
          <div
            className={`rounded-2xl border px-2.5 py-0.5 text-sm ${isCurrentUser ? 'border-gray-300 bg-gray-50' : 'border-gray-800 bg-gray-800 text-white'}`}
          >
            {color === 'WHITE' ? '백돌' : '흑돌'}
          </div>
        </div>
        <div className="flex justify-between gap-2">
          <div className="flex items-center gap-0.5 font-semibold">
            {[...Array(remaining)].map((_, idx) => (
              <div
                key={idx}
                className={`h-5 w-5 rounded-full border ${color === 'WHITE' ? 'border-gray-500 bg-gray-50' : 'border-gray-800 bg-gray-800'}`}
              />
            ))}
            {[...Array(9 - remaining)].map((_, idx) => (
              <div key={idx} className={`h-5 w-5 rounded-full bg-gray-200`} />
            ))}
          </div>
          <span className="px-2 font-semibold">{remaining} / 9</span>
        </div>
        {isCurrentUser && (
          <div className="flex gap-2">
            <Button
              text="기권하기"
              icon={<Flag />}
              slim
              fullWidth
              onClick={onWithdraw}
            />
            <Button
              theme="secondary"
              text="도움말"
              icon={<Help />}
              slim
              fullWidth
            />
          </div>
        )}
      </div>
    </div>
  );
}
