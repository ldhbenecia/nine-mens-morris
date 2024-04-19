import Play from '~/assets/icons/play.svg?react';
import Flag from '~/assets/icons/flag.svg?react';
import Help from '~/assets/icons/help.svg?react';
import { Button } from '~/components';

type StatusProps = {
  isCurrentUser?: boolean;
  isTurn: boolean;
  color: 'WHITE' | 'BLACK';
  addable: number;
  total: number;
  visible?: boolean;
  onShowWithdrawModal?: () => void;
  onShowHelpModal?: () => void;
};

export function Status({
  isCurrentUser = false,
  isTurn,
  color,
  addable,
  total,
  visible = true,
  onShowWithdrawModal,
  onShowHelpModal,
}: StatusProps) {
  return (
    <div
      className={`z-10 flex md:translate-x-0 ${visible ? 'visible' : 'invisible'}`}
    >
      <div className={`gap-2 ${isTurn ? '' : 'opacity-0'}`}>
        <Play />
      </div>
      <div className="flex w-48 flex-col gap-1">
        <div className="flex items-center justify-between">
          <span className="text-lg font-semibold">
            {isCurrentUser ? '나' : '상대'}
          </span>
          <div
            className={`rounded-2xl border px-1.5 text-sm ${color === 'WHITE' ? 'border-gray-300 bg-gray-50' : 'border-gray-800 bg-gray-800 text-white'}`}
          >
            {color === 'WHITE' ? '백돌' : '흑돌'}
          </div>
        </div>
        <div className="flex justify-between">
          <div className="flex items-center gap-0.5 font-semibold">
            {[...Array(addable)].map((_, idx) => (
              <div
                key={idx}
                className={`h-3.5 w-3.5 rounded-full border ${color === 'WHITE' ? 'border-gray-500 bg-gray-50' : 'border-gray-800 bg-gray-800'}`}
              />
            ))}
            {[...Array(9 - addable)].map((_, idx) => (
              <div
                key={idx}
                className={`h-3.5 w-3.5 rounded-full bg-gray-800 opacity-10`}
              />
            ))}
          </div>
          <span className="px-1 text-sm font-semibold">
            {addable} / {total}
          </span>
        </div>
        {isCurrentUser && (
          <div className="mt-2 flex gap-2">
            <Button
              text="기권하기"
              icon={<Flag width={18} height={18} />}
              slim
              small
              fullWidth
              onClick={onShowWithdrawModal}
            />
            <Button
              theme="secondary"
              text="도움말"
              icon={<Help width={18} height={18} />}
              slim
              small
              fullWidth
              onClick={onShowHelpModal}
            />
          </div>
        )}
      </div>
    </div>
  );
}
