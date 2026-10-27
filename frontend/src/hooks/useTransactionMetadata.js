import { useEffect, useState } from 'react';
import { getTransactionMetadata } from '../api/transactions';

export default function useTransactionMetadata() {
  const [attempt, setAttempt] = useState(0);
  const [state, setState] = useState({ status: 'loading', data: null, error: '' });

  useEffect(() => {
    const controller = new AbortController();
    getTransactionMetadata(controller.signal).then(
      (data) => {
        if (!controller.signal.aborted) setState({ status: 'ready', data, error: '' });
      },
      (error) => {
        if (!controller.signal.aborted) setState({ status: 'error', data: null, error: error.message });
      },
    );
    return () => controller.abort();
  }, [attempt]);

  function retry() {
    setState({ status: 'loading', data: null, error: '' });
    setAttempt((value) => value + 1);
  }

  return { ...state, retry };
}
