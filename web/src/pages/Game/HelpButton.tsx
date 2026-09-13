import Help from '~/assets/icons/help.svg?react';
import { clickSound } from '~/lib/sounds';

type HelpButtonProps = {
  onClick?: () => void;
};

export function HelpButton({ onClick }: HelpButtonProps) {
  const onClickWithSound = () => {
    onClick?.();
    clickSound.play();
  };

  return (
    <div
      onClick={onClickWithSound}
      className="cursor-pointer rounded-full border border-gray-300 bg-gray-50 p-1.5 hover:bg-white active:bg-gray-100"
    >
      <Help width={18} height={18} className="fill-white" />
    </div>
  );
}
