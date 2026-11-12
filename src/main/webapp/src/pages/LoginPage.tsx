import { Alert, Button, Form, Input } from 'antd';
import { useState } from 'react';
import { t } from '../i18n';
import { errorMessage, fetchStatus, getApiKey, isUnauthorized, setApiKey } from '../services/api';

/** Asks for the API key and verifies it against the server before continuing. */
export function LoginPage({ onConnected }: { onConnected: () => void }) {
  const [value, setValue] = useState(getApiKey());
  const [checking, setChecking] = useState(false);
  const [problem, setProblem] = useState<string>();

  const connect = async () => {
    setChecking(true);
    setProblem(undefined);
    setApiKey(value.trim());
    try {
      await fetchStatus();
      onConnected();
    } catch (error) {
      setProblem(isUnauthorized(error) ? t('apiKeyInvalid') : errorMessage(error));
    } finally {
      setChecking(false);
    }
  };

  return (
    <div className="login">
      <h1>tinyMediaManager</h1>
      <Form layout="vertical" onFinish={connect}>
        <Form.Item label={t('apiKey')} extra={t('apiKeyHelp')}>
          <Input.Password
            size="large"
            autoComplete="off"
            autoCapitalize="none"
            spellCheck={false}
            value={value}
            onChange={(event) => setValue(event.target.value)}
          />
        </Form.Item>
        {problem && <Alert type="error" showIcon title={problem} className="login-problem" />}
        <Button type="primary" size="large" block htmlType="submit" loading={checking} disabled={!value.trim()}>
          {t('connect')}
        </Button>
      </Form>
    </div>
  );
}
