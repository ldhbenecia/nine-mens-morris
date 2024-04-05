type ButtonProps = {
  text: string;
  onClick?: React.MouseEventHandler<HTMLButtonElement>;
  theme?: 'primary' | 'secondary';
  icon?: React.ReactNode;
  slim?: boolean;
  fullWidth?: boolean;
};

export function Button({
  text,
  onClick,
  theme = 'primary',
  icon,
  slim = false,
  fullWidth = false,
}: ButtonProps) {
  return (
    <button
      className={`flex cursor-default items-center justify-center gap-1 rounded-lg font-semibold leading-tight ${fullWidth ? 'w-full' : ''} ${slim ? 'px-3 py-1.5' : 'p-3'} ${theme === 'primary' ? 'border border-gray-800 bg-gray-800 text-white hover:bg-gray-700 active:bg-gray-900' : 'border border-gray-300 bg-gray-50 text-black hover:bg-white active:bg-gray-100'}`}
      onClick={onClick}
    >
      {icon}
      {text}
    </button>
  );
}
