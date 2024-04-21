import Play from '~/assets/icons/play.svg?react';
import Flag from '~/assets/icons/flag.svg?react';
import HandShake from '~/assets/icons/handshake.svg?react';
import { Button } from '~/components';
import { HelpButton } from './HelpButton';

type StatusProps = {
  isCurrentUser?: boolean;
  turn: boolean;
  color: 'WHITE' | 'BLACK';
  addable: number;
  total: number;
  nickname?: string;
  visible?: boolean;
  onShowWithdrawModal?: () => void;
  onShowRequestDrawModal?: () => void;
  onShowHelpModal?: () => void;
};

export function Status({
  isCurrentUser = false,
  turn,
  color,
  addable,
  total,
  nickname = ' ',
  visible = true,
  onShowWithdrawModal,
  onShowRequestDrawModal,
  onShowHelpModal,
}: StatusProps) {
  return (
    <div
      className={`z-10 flex transition-opacity md:translate-x-0 ${visible ? 'visible' : 'invisible'}`}
    >
      <div className={`gap-2 ${turn ? '' : 'opacity-0'}`}>
        <Play />
      </div>
      <div className="flex w-56 flex-col gap-1">
        <div
          className={`flex items-center justify-between  ${turn ? '' : 'opacity-50'}`}
        >
          <span className="text-lg font-semibold">{nickname}</span>
          <div
            className={`rounded-2xl border px-1.5 text-sm ${color === 'WHITE' ? 'border-gray-300 bg-gray-50' : 'border-gray-800 bg-gray-800 text-white'}`}
          >
            {color === 'WHITE' ? '백돌' : '흑돌'}
          </div>
        </div>
        <div className={`flex justify-between ${turn ? '' : 'opacity-50'}`}>
          <div className="flex items-center gap-0.5 font-semibold">
            {[...Array(addable)].map((_, idx) => (
              <div
                key={idx}
                className={`h-4 w-4 rounded-full border ${color === 'WHITE' ? 'border-gray-500 bg-gray-50' : 'border-gray-800 bg-gray-800'}`}
              />
            ))}
            {[...Array(9 - addable)].map((_, idx) => (
              <div
                key={idx}
                className={`h-4 w-4 rounded-full bg-gray-800 opacity-10`}
              />
            ))}
          </div>
          <span className="text-nowrap px-1 text-sm font-semibold">
            {addable} / {total}
          </span>
        </div>
        {isCurrentUser && (
          <div className="mt-2 flex gap-2">
            <div className="flex grow gap-2">
              <Button
                text="기권"
                icon={<Flag width={18} height={18} />}
                slim
                small
                fullWidth
                onClick={onShowWithdrawModal}
              />
              <Button
                text="무승부"
                icon={<HandShake width={18} height={18} />}
                slim
                small
                fullWidth
                onClick={onShowRequestDrawModal}
              />
            </div>
            <HelpButton onClick={onShowHelpModal} />
          </div>
        )}
      </div>
    </div>
  );
}
