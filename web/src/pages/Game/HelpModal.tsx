import { useState } from 'react';
import { Button, Modal } from '~/components';

type HelpModalProps = {
  visible: boolean;
  onClose: () => void;
};

const contents = [
  {
    imageSrc: '/src/assets/images/example_1.png',
    text: (
      <>
        <div>
          <b>나인 멘스 모리스</b>는 두 명의 플레이어끼리 즐길 수 있는 간단한
          규칙의 게임입니다.
        </div>
        <div>
          처음에 각자 지급받는 <b>9개의 흑돌과 백돌</b>을 이용하여 상대의 돌을
          줄여나가는 것이 이 게임의 목표입니다.
        </div>
      </>
    ),
  },
  {
    imageSrc: '/src/assets/images/example_3.png',
    text: (
      <>
        <div>
          게임은 총 <b>2개의 페이즈</b>로 진행됩니다.
        </div>
        <div>
          <b>1페이즈</b>에서는 각자의 돌을 하나씩 번갈아가며 보드 위에 두게
          되고, 9개의 돌을 모두 배치하면 다음 페이즈로 넘어갑니다.
        </div>
        <div>
          <b>2페이즈</b>에서는 보드 위에 배치한 자신의 돌 중 하나를 선택하여
          인접한 칸으로 이동시킬 수 있습니다.
        </div>
      </>
    ),
  },
  {
    imageSrc: '/src/assets/images/example_2.png',
    text: (
      <>
        <div>
          이때 돌을 배치하는 과정에서 <b>가로 또는 세로로 연속 3개 배열</b>이
          만들어지는 경우, 상대의 돌 중 하나를 골라 제거할 수 있습니다.
        </div>
        <div>
          단, 이때 제거하려는 돌은 <b>연속 3개 배열</b>에 속하지 않아야 하며,
          가장 먼저 남은 돌이 <b>2개 이하</b>가 된 쪽이 패배합니다.
        </div>
      </>
    ),
  },
];

export function HelpModal({ visible, onClose }: HelpModalProps) {
  const [page, setPage] = useState(0);

  const onPrev = () => {
    if (page > 0) {
      setPage(page - 1);
      return;
    }

    onClose();
  };

  const onNext = () => {
    if (page < 2) {
      setPage(page + 1);
      return;
    }

    setTimeout(() => setPage(0), 500);
    onClose();
  };

  return (
    <Modal visible={visible}>
      <>
        <div className="text-2xl font-semibold">도움말</div>
        <div className="flex flex-col items-center justify-between gap-4 md:flex-row">
          <img
            src={contents[page].imageSrc}
            className="h-64 w-64 items-center rounded-lg"
          />
          <div className="flex w-80 flex-col gap-2">
            {contents[page].text}{' '}
            <span className="text-sm text-gray-500">({page + 1}/3)</span>
          </div>
        </div>
        <div className="flex w-full gap-4">
          <Button theme="secondary" fullWidth text="이전" onClick={onPrev} />
          <Button
            fullWidth
            text={page === 2 ? '확인' : '다음'}
            onClick={onNext}
          />
        </div>
      </>
    </Modal>
  );
}
