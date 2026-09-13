import { clickSound } from '~/lib/sounds';

type ButtonProps = {
  text: string;
  onClick?: () => void;
  theme?: 'primary' | 'secondary';
  icon?: React.ReactNode;
  slim?: boolean;
  fullWidth?: boolean;
  small?: boolean;
  disabled?: boolean;
};

export function Button({
  text,
  onClick,
  theme = 'primary',
  icon,
  slim = false,
  fullWidth = false,
  small = false,
  disabled = false,
}: ButtonProps) {
  const onClickWithSound = () => {
    if (disabled) return;
    onClick?.();
    clickSound.play();
  };

  return (
    <button
      disabled={disabled}
      className={`flex items-center justify-center gap-1 rounded-lg font-semibold leading-tight ${fullWidth ? 'w-full' : ''} ${small ? 'text-sm' : ''} ${slim ? 'px-2 py-1.5' : 'p-3'} ${disabled ? 'cursor-not-allowed border border-gray-300 bg-gray-200 text-gray-500' : theme === 'primary' ? 'cursor-pointer border border-gray-800 bg-gray-800 text-white hover:bg-gray-700 active:bg-gray-900' : 'cursor-pointer border border-gray-300 bg-gray-50 text-black hover:bg-white active:bg-gray-100'}`}
      onClick={onClickWithSound}
    >
      {icon}
      {text}
    </button>
  );
}
