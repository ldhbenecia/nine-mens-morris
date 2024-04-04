type ButtonProps = {
  theme?: 'primary' | 'secondary';
  icon?: React.ReactNode;
  text: string;
  onClick: React.MouseEventHandler<HTMLButtonElement>;
};

export function Button({
  theme = 'primary',
  icon,
  text,
  onClick,
}: ButtonProps) {
  return (
    <button
      className={`flex cursor-default justify-center gap-1 rounded-lg p-3 font-default text-lg font-semibold ${theme === 'primary' ? 'border border-gray-800 bg-gray-800 text-white hover:bg-gray-700 active:bg-gray-900' : 'border border-gray-300 bg-gray-50 text-black hover:bg-white active:bg-gray-100'}`}
      onClick={onClick}
    >
      {icon}
      {text}
    </button>
  );
}
