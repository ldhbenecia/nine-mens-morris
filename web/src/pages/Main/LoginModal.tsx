import { useState } from 'react';
import { Button, KakaoButton, Modal } from '~/components';
import { useStartAsVisitor } from '~/hooks';

type LoginModalProps = {
  visible: boolean;
  onClose: () => void;
};

// 로그인이 진입 장벽이 되지 않게 게스트를 기본 버튼으로 둔다
// 대신 "랭킹에 오르려면 로그인" 이라는 이유를 남겨 로그인을 강제하지 않고도 유인을 만든다
export function LoginModal({ visible, onClose }: LoginModalProps) {
  const [failure, setFailure] = useState('');
  const { mutate: startAsVisitor, isPending } = useStartAsVisitor(setFailure);

  return (
    <Modal visible={visible}>
      <>
        <div className="font-semibold">어떻게 시작할까요?</div>
        <div className="flex w-full flex-col gap-4">
          <Button
            text={isPending ? '준비 중...' : '게스트로 바로 시작'}
            disabled={isPending}
            fullWidth
            onClick={() => startAsVisitor()}
          />
          <KakaoButton />
          <span className="text-center text-xs text-gray-500">
            랭킹에 오르려면 카카오 로그인이 필요합니다
          </span>
          {failure && (
            <span className="text-center text-xs text-red-600">{failure}</span>
          )}
          <Button theme="secondary" text="취소" onClick={onClose} fullWidth />
        </div>
      </>
    </Modal>
  );
}
