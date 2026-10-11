import { useEffect, useState } from 'react';

// 마감 시각(내 시계 기준 ms)까지 남은 초. 마감이 없으면 null
// 초가 바뀌는 순간을 놓치지 않게 1초보다 촘촘히 깨어남
export function useCountdown(deadline: number | null) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    if (deadline === null) return;
    const timer = window.setInterval(() => setNow(Date.now()), 250);
    return () => window.clearInterval(timer);
  }, [deadline]);

  if (deadline === null) return null;
  return Math.max(0, Math.ceil((deadline - now) / 1000));
}
