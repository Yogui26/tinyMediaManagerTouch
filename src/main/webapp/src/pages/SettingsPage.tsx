import { Button, Tag } from 'antd';
import { ErrorView, Loading } from '../components/StateViews';
import { useAsync } from '../hooks/useAsync';
import { t } from '../i18n';
import { fetchStatus, setApiKey } from '../services/api';

/** Shows the connection state and lets the user remove the API key from this device. */
export function SettingsPage({ onSignedOut }: { onSignedOut: () => void }) {
  const { data: status, error, loading, reload } = useAsync((signal) => fetchStatus(signal), []);

  if (error !== undefined) {
    return <ErrorView error={error} onRetry={reload} />;
  }
  if (!status || loading) {
    return <Loading />;
  }

  return (
    <div className="settings">
      <h1>{t('settings')}</h1>
      <p>
        {t('connected', { version: status.version })} <Tag color={status.busy ? 'gold' : 'green'}>{status.busy ? t('busy') : t('idle')}</Tag>
      </p>
      <Button
        size="large"
        danger
        onClick={() => {
          setApiKey('');
          onSignedOut();
        }}
      >
        {t('forgetKey')}
      </Button>
    </div>
  );
}
