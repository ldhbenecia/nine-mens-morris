type ModalProps = {
  children: React.ReactElement;
};

export function Modal({ children }: ModalProps) {
  return (
    <div className="fixed flex h-full w-full animate-modal-background items-center justify-center bg-[rgba(0,0,0,0.2)]">
      <div className="flex w-80 max-w-full animate-modal flex-col items-center gap-6 rounded-2xl border border-gray-300 bg-white p-8 shadow-lg">
        {children}
      </div>
    </div>
  );
}
