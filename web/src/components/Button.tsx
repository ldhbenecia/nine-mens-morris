import { clickSound } from '~/lib/sounds';

type ButtonProps = {
  text: string;
  onClick: () => void;
  theme?: 'primary' | 'secondary';
  icon?: React.ReactNode;
  slim?: boolean;
  fullWidth?: boolean;
  small?: boolean;
};

export function Button({
  text,
  onClick,
  theme = 'primary',
  icon,
  slim = false,
  fullWidth = false,
  small = false,
}: ButtonProps) {
  const onClickWithSound = () => {
    onClick();
    clickSound.play();
  };

  return (
    <button
      className={`flex cursor-pointer items-center justify-center gap-1 rounded-lg font-semibold leading-tight ${fullWidth ? 'w-full' : ''} ${small ? 'text-sm' : ''} ${slim ? 'px-2 py-1.5' : 'p-3'} ${theme === 'primary' ? 'border border-gray-800 bg-gray-800 text-white hover:bg-gray-700 active:bg-gray-900' : 'border border-gray-300 bg-gray-50 text-black hover:bg-white active:bg-gray-100'}`}
      onClick={onClickWithSound}
    >
      <audio src="/src/assets/sounds/click.mp3" />
      {icon}
      {text}
    </button>
  );
}
