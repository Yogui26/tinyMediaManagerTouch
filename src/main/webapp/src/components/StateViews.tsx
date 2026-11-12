import { Button, Result, Spin } from 'antd';
import { t } from '../i18n';
import { errorMessage } from '../services/api';

/** A centered loading indicator. */
export function Loading() {
  return (
    <div className="centered">
      <Spin size="large" />
    </div>
  );
}

/** A user-friendly error message with a retry button. */
export function ErrorView({ error, onRetry }: { error: unknown; onRetry: () => void }) {
  return (
    <Result
      status="warning"
      title={errorMessage(error)}
      extra={
        <Button type="primary" size="large" onClick={onRetry}>
          {t('retry')}
        </Button>
      }
    />
  );
}
