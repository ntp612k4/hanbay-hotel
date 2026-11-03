import { useCallback, useEffect, useState } from 'react';

export function useLoginCooldown() {
  const [deadline, setDeadline] = useState(0);
  const [seconds, setSeconds] = useState(0);
  const start = useCallback((duration: number) => {
    const safeDuration = Number.isFinite(duration) && duration >= 0 ? duration : 60;
    setDeadline(Date.now() + safeDuration * 1000);
    setSeconds(Math.ceil(safeDuration));
  }, []);
  useEffect(() => {
    if (!deadline) return;
    const update = () => {
      const remaining = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
      setSeconds(remaining);
      if (remaining === 0) setDeadline(0);
    };
    const timer = window.setInterval(update, 1000);
    window.addEventListener('focus', update);
    document.addEventListener('visibilitychange', update);
    return () => {
      window.clearInterval(timer);
      window.removeEventListener('focus', update);
      document.removeEventListener('visibilitychange', update);
    };
  }, [deadline]);
  return { seconds, start };
}
