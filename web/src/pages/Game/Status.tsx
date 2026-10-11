import Flag from '~/assets/icons/flag.svg?react';
import HandShake from '~/assets/icons/handshake.svg?react';
import { Avatar, Button } from '~/components';
import { HelpButton } from './HelpButton';
import { StoneType, UserProfile } from '~/lib/types';
import { TIER_LABEL } from '~/lib/tier';

type StatusProps = {
  isCurrentUser?: boolean;
  turn: boolean;
  // 시작 전에는 흑백이 정해지지 않아 EMPTY 가 온다
  color: StoneType;
  inHand: number;
  onBoard: number;
  player?: Pick<
    UserProfile,
    'nickname' | 'imageUrl' | 'mmr' | 'tier' | 'visitor'
  >;
  onShowResignModal?: () => void;
  onShowRequestDrawModal?: () => void;
  onShowHelpModal?: () => void;
};

export function Status({
  isCurrentUser = false,
  turn,
  color,
  inHand,
  onBoard,
  player,
  onShowResignModal,
  onShowRequestDrawModal,
  onShowHelpModal,
}: StatusProps) {
  const nickname = player?.nickname ?? '';

  return (
    <div className="z-10 w-64">
      <div
        className={`flex flex-col gap-2 transition-opacity duration-300 ${turn ? '' : 'opacity-50'}`}
      >
        <div className="flex items-center gap-3">
          <div className="relative">
            {turn && (
              <div className="absolute -inset-1 animate-pulse rounded-full ring-2 ring-gray-800" />
            )}
            <Avatar nickname={nickname || ' '} imageUrl={player?.imageUrl} />
            {/* 아바타만 봐도 어느 색인지 알게 돌을 겹쳐 그림 */}
            {color !== 'EMPTY' && (
              <div
                className={`absolute -right-1 -bottom-1 h-4 w-4 rounded-full border-2 border-white shadow-stone ${color === 'WHITE' ? 'bg-white-stone' : 'bg-black-stone'}`}
              />
            )}
          </div>
          <div className="flex min-w-0 grow flex-col leading-tight">
            <span className="truncate font-semibold">{nickname}</span>
            {/* 게스트는 MMR 이 움직이지 않음. 티어를 보여주면 오르는 값처럼 읽힘 */}
            {player && (
              <span className="text-xs font-semibold text-gray-500">
                {player.visitor
                  ? '게스트'
                  : `${TIER_LABEL[player.tier]} · ${player.mmr.toLocaleString()}`}
              </span>
            )}
          </div>
          <div
            className={`shrink-0 rounded-2xl border px-1.5 text-sm ${color === 'WHITE' ? 'border-gray-300 bg-gray-50' : 'border-gray-800 bg-gray-800 text-white'}`}
          >
            {color === 'EMPTY' ? '대기' : color === 'WHITE' ? '백돌' : '흑돌'}
          </div>
        </div>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-0.5">
            {[...Array(inHand)].map((_, idx) => (
              <div
                key={idx}
                className={`h-3.5 w-3.5 rounded-full border ${color === 'WHITE' ? 'border-gray-500 bg-gray-50' : 'border-gray-800 bg-gray-800'}`}
              />
            ))}
            {[...Array(Math.max(0, 9 - inHand))].map((_, idx) => (
              <div
                key={idx}
                className="h-3.5 w-3.5 rounded-full bg-gray-800 opacity-10"
              />
            ))}
          </div>
          <span className="text-xs font-semibold text-nowrap text-gray-600">
            손 {inHand} / 판 {onBoard}
          </span>
        </div>
      </div>
      {isCurrentUser && (
        <div className="mt-3 flex gap-2">
          <div className="flex grow gap-2">
            <Button
              text="기권"
              icon={<Flag width={18} height={18} />}
              slim
              small
              fullWidth
              onClick={onShowResignModal}
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
  );
}
