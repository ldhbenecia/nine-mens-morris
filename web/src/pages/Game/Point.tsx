type PointProps = {
  stone?: 'EMPTY' | 'WHITE' | 'BLACK';
  selected?: boolean;
  unused?: boolean;
};

export function Point({
  stone = 'EMPTY',
  selected = false,
  unused = false,
}: PointProps) {
  if (unused) {
    return <div className="h-16 w-16" />;
  }
  if (stone === 'EMPTY') {
    return (
      <div className="flex h-16 w-16 items-center justify-center rounded-full transition-colors hover:bg-[rgba(107,114,128,0.1)]">
        <div className="h-2 w-2 rounded-full bg-gray-400 " />
      </div>
    );
  }

  return (
    <div
      className={`flex h-16 w-16 items-center justify-center rounded-full ${selected ? 'border-2 border-dashed border-red-600 bg-[rgba(255,0,0,0.1)]' : ''}`}
    >
      <div
        className={`h-full w-full rounded-full shadow-md  ${stone === 'WHITE' ? 'bg-white-stone' : 'bg-black-stone'}  ${selected ? 'opacity-50' : ''}`}
      />
    </div>
  );
}
