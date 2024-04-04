import { useState } from 'react';
import { Button } from '~/components/Button';
import { KakaoButton } from '~/components/KakaoButton';
import { Modal } from '~/components/Modal';

export function MainPage() {
  const [showModal, setShowModal] = useState(false);

  return (
    <main className="flex h-full flex-col items-center justify-center gap-12">
      {showModal && (
        <Modal>
          <>
            <div className="text-lg font-semibold">로그인이 필요합니다.</div>
            <div className="flex w-full flex-col gap-4">
              <KakaoButton />
              <Button
                theme="secondary"
                text="취소"
                onClick={() => setShowModal(false)}
              />
            </div>
          </>
        </Modal>
      )}
      <div className="flex flex-col items-center gap-2">
        <h1 className="font-title text-5xl">Nine Men&apos;s Morris</h1>
        <h2 className="text-2xl font-light tracking-[0.8rem] text-gray-500">
          나인 멘스 모리스
        </h2>
      </div>
      <div className="flex w-60 flex-col gap-4">
        <Button text="게임 시작" onClick={() => setShowModal(true)} />
        <Button theme="secondary" text="랭킹 보기" onClick={() => {}} />
      </div>
    </main>
  );
}
