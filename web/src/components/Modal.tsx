type ModalProps = {
  children: React.ReactElement;
};

export function Modal({ children }: ModalProps) {
  return (
    <div className="animate-modal-background fixed flex h-full w-full items-center justify-center bg-[rgba(0,0,0,0.2)]">
      <div className="animate-modal flex min-w-[22.5rem] flex-col items-center gap-6 rounded-2xl border border-gray-300 bg-white p-8 shadow-lg">
        {children}
      </div>
    </div>
  );
}
