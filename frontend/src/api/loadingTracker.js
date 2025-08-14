let activeCount = 0;
const listeners = new Set();

export const subscribeLoading = (listener) => {
  listeners.add(listener);
  return () => listeners.delete(listener);
};

const notify = () => {
  for (const l of listeners) {
    try { l(activeCount); } catch {}
  }
};

export const incrementLoading = () => {
  activeCount += 1;
  notify();
};

export const decrementLoading = () => {
  activeCount = Math.max(0, activeCount - 1);
  notify();
};

export const getLoadingCount = () => activeCount;


