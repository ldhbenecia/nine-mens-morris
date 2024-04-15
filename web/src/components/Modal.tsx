import { useRef } from 'react';
import { Transition } from 'react-transition-group';

type ModalProps = {
  visible: boolean;
  children: React.ReactElement;
};

export function Modal({ visible, children }: ModalProps) {
  const nodeRef = useRef(null);

  return (
    <Transition in={visible} timeout={150} unmountOnExit nodeRef={nodeRef}>
      {(state) => (
        <div
          ref={nodeRef}
          className="fixed left-0 top-0 z-50 flex h-full w-full items-center justify-center bg-[rgba(0,0,0,0.2)] transition-opacity"
          style={{
            opacity: state === 'exited' || state === 'exiting' ? 0 : 1,
            pointerEvents: state === 'entered' ? 'auto' : 'none',
          }}
        >
          <div className="flex w-80 max-w-full animate-modal flex-col items-center gap-6 rounded-2xl border border-gray-300 bg-white p-8 shadow-lg">
            {children}
          </div>
        </div>
      )}
    </Transition>
  );
}
