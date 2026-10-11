import { useEffect, useState } from 'react';

// from 에서 to 까지 숫자가 올라가는 연출. 끝으로 갈수록 느려짐
export function useCountUp(from: number, to: number, durationMs = 800) {
  const [value, setValue] = useState(from);

  useEffect(() => {
    let frame = 0;
    const start = performance.now();
    const tick = (now: number) => {
      const progress = Math.min(1, (now - start) / durationMs);
      const eased = 1 - (1 - progress) ** 3;
      setValue(Math.round(from + (to - from) * eased));
      if (progress < 1) frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [from, to, durationMs]);

  return value;
}
