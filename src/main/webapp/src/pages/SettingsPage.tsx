import { App as AntApp, Button, Progress, Tag } from 'antd';
import { useEffect, useState } from 'react';
import { ErrorView, Loading } from '../components/StateViews';
import { t } from '../i18n';
import { errorMessage, fetchStatus, isCancel, scrapeNew, setApiKey, updateDataSources } from '../services/api';
import type { LibraryModule, SystemStatus } from '../services/api';

const POLL_INTERVAL = 3000;

/** Polls the system status as long as the page is shown. */
function useStatus() {
  const [status, setStatus] = useState<SystemStatus>();
  const [error, setError] = useState<unknown>();
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let timer: number | undefined;

    const poll = () => {
      fetchStatus(controller.signal)
        .then((result) => {
          setStatus(result);
          setError(undefined);
          timer = window.setTimeout(poll, POLL_INTERVAL);
        })
        .catch((reason: unknown) => {
          if (!isCancel(reason)) {
            setError(reason);
          }
        });
    };
    poll();

    return () => {
      controller.abort();
      window.clearTimeout(timer);
    };
  }, [attempt]);

  return { status, error, retry: () => setAttempt((value) => value + 1) };
}

/** Connection state, library actions, running tasks and sign-out. */
export function SettingsPage({ onSignedOut }: { onSignedOut: () => void }) {
  const { status, error, retry } = useStatus();
  const { message } = AntApp.useApp();
  const [pending, setPending] = useState<string>();

  if (error !== undefined) {
    return <ErrorView error={error} onRetry={retry} />;
  }
  if (!status) {
    return <Loading />;
  }

  const run = async (name: string, command: (module: LibraryModule) => Promise<void>) => {
    setPending(name);
    try {
      await command('movie');
      await command('tvshow');
      message.success(t('commandQueued'));
    } catch (reason) {
      message.error(errorMessage(reason));
    } finally {
      setPending(undefined);
    }
  };

  return (
    <div className="settings">
      <h1>{t('settings')}</h1>
      <p>
        {t('connected', { version: status.version })} <Tag color={status.busy ? 'gold' : 'green'}>{status.busy ? t('busy') : t('idle')}</Tag>
      </p>

      <section className="settings-section">
        <h2>{t('library')}</h2>
        <Button type="primary" size="large" block loading={pending === 'update'} disabled={pending !== undefined} onClick={() => run('update', updateDataSources)}>
          {t('updateLibrary')}
        </Button>
        <p className="settings-help">{t('updateLibraryHelp')}</p>
        <Button size="large" block loading={pending === 'scrape'} disabled={pending !== undefined} onClick={() => run('scrape', scrapeNew)}>
          {t('scrapeNew')}
        </Button>
        <p className="settings-help">{t('scrapeNewHelp')}</p>
      </section>

      <section className="settings-section">
        <h2>{t('runningTasks')}</h2>
        {status.runningTasks.length === 0 ? (
          <p className="settings-help">{t('noTasks')}</p>
        ) : (
          <ul className="tasks">
            {status.runningTasks.map((task, index) => (
              <li key={`${task.name}-${index}`}>
                <div className="task-name">{task.name}</div>
                {task.description && <div className="settings-help">{task.description}</div>}
                <Progress
                  percent={task.workUnits > 0 ? Math.round((task.progressDone / task.workUnits) * 100) : 0}
                  status="active"
                  showInfo={task.workUnits > 0}
                />
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="settings-section">
        <Button
          size="large"
          danger
          block
          onClick={() => {
            setApiKey('');
            onSignedOut();
          }}
        >
          {t('forgetKey')}
        </Button>
      </section>
    </div>
  );
}
