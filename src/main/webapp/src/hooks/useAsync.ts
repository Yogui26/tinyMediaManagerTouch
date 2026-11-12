import { useCallback, useEffect, useState } from 'react';
import { isCancel } from '../services/api';

export interface AsyncState<T> {
  data: T | undefined;
  error: unknown;
  loading: boolean;
  reload: () => void;
}

/**
 * Runs the given loader whenever one of the dependencies changes and aborts the previous request.
 */
export function useAsync<T>(loader: (signal: AbortSignal) => Promise<T>, deps: unknown[]): AsyncState<T> {
  const [state, setState] = useState<{ data: T | undefined; error: unknown; loading: boolean }>({
    data: undefined,
    error: undefined,
    loading: true,
  });
  const [generation, setGeneration] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setState((previous) => ({ ...previous, error: undefined, loading: true }));

    loader(controller.signal)
      .then((data) => setState({ data, error: undefined, loading: false }))
      .catch((error: unknown) => {
        if (!isCancel(error)) {
          setState({ data: undefined, error, loading: false });
        }
      });

    return () => controller.abort();
    // the loader is intentionally not a dependency - the caller controls reloading via deps
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, generation]);

  const reload = useCallback(() => setGeneration((value) => value + 1), []);

  return { ...state, reload };
}
