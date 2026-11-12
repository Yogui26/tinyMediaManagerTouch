import { DesktopOutlined, SettingOutlined, VideoCameraOutlined } from '@ant-design/icons';
import { useCallback, useEffect, useState } from 'react';
import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import { ErrorView, Loading } from './components/StateViews';
import { t } from './i18n';
import { LibraryPage } from './pages/LibraryPage';
import { LoginPage } from './pages/LoginPage';
import { MovieDetailPage } from './pages/MovieDetailPage';
import { SettingsPage } from './pages/SettingsPage';
import { TvShowDetailPage } from './pages/TvShowDetailPage';
import { UNAUTHORIZED_EVENT, fetchStatus, isUnauthorized } from './services/api';

type Connection = { state: 'checking' } | { state: 'login' } | { state: 'ready' } | { state: 'error'; error: unknown };

/** The application shell: connection handling, navigation and routes. */
export function App() {
  const [connection, setConnection] = useState<Connection>({ state: 'checking' });

  const check = useCallback(() => {
    setConnection({ state: 'checking' });
    fetchStatus()
      .then(() => setConnection({ state: 'ready' }))
      .catch((error: unknown) => setConnection(isUnauthorized(error) ? { state: 'login' } : { state: 'error', error }));
  }, []);

  useEffect(() => {
    check();
    const onUnauthorized = () => setConnection({ state: 'login' });
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
  }, [check]);

  if (connection.state === 'checking') {
    return <Loading />;
  }
  if (connection.state === 'error') {
    return <ErrorView error={connection.error} onRetry={check} />;
  }
  if (connection.state === 'login') {
    return <LoginPage onConnected={() => setConnection({ state: 'ready' })} />;
  }

  return (
    <div className="shell">
      <nav className="nav" aria-label="Main">
        <span className="nav-brand">tinyMediaManager</span>
        <NavLink to="/movies">
          <VideoCameraOutlined />
          <span>{t('movies')}</span>
        </NavLink>
        <NavLink to="/tvshows">
          <DesktopOutlined />
          <span>{t('tvShows')}</span>
        </NavLink>
        <NavLink to="/settings">
          <SettingOutlined />
          <span>{t('settings')}</span>
        </NavLink>
      </nav>
      <main className="content">
        <Routes>
          <Route path="/movies" element={<LibraryPage key="movies" kind="movies" />} />
          <Route path="/movies/:id" element={<MovieDetailPage />} />
          <Route path="/tvshows" element={<LibraryPage key="tvshows" kind="tvshows" />} />
          <Route path="/tvshows/:id" element={<TvShowDetailPage />} />
          <Route path="/settings" element={<SettingsPage onSignedOut={() => setConnection({ state: 'login' })} />} />
          <Route path="*" element={<Navigate to="/movies" replace />} />
        </Routes>
      </main>
    </div>
  );
}
