import { ArrowLeftOutlined, CheckCircleFilled, StarFilled } from '@ant-design/icons';
import { Button, Tag } from 'antd';
import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArtworkImage } from '../components/ArtworkImage';
import { t } from '../i18n';
import type { MediaSummary, Person } from '../services/api';

interface DetailLayoutProps {
  /** the API base path of the entity, e.g. /v2/movies/{id} */
  basePath: string;
  /** the route to go back to if there is no browser history */
  parentRoute: string;
  media: MediaSummary;
  tagline?: string;
  facts: string[];
  children: ReactNode;
}

/** The shared frame of the movie and TV show detail pages: backdrop, poster, title and key facts. */
export function DetailLayout({ basePath, parentRoute, media, tagline, facts, children }: DetailLayoutProps) {
  const navigate = useNavigate();
  const goBack = () => (window.history.state?.idx > 0 ? navigate(-1) : navigate(parentRoute));

  return (
    <article className="detail">
      <div className="detail-hero">
        {media.hasFanart && <ArtworkImage path={`${basePath}/artwork/fanart`} alt="" className="detail-fanart" />}
        <div className="detail-hero-shade" />
        <Button className="detail-back" size="large" shape="circle" icon={<ArrowLeftOutlined />} aria-label={t('back')} onClick={goBack} />
      </div>

      <header className="detail-header">
        <ArtworkImage path={media.hasPoster ? `${basePath}/artwork/poster` : undefined} alt={media.title} className="detail-poster" />
        <div className="detail-heading">
          <h1>{media.title}</h1>
          {media.originalTitle && media.originalTitle !== media.title && <div className="detail-original">{media.originalTitle}</div>}
          {tagline && <div className="detail-tagline">{tagline}</div>}
          <div className="detail-facts">
            {media.rating > 0 && (
              <span className="detail-rating">
                <StarFilled /> {media.rating.toFixed(1)}
              </span>
            )}
            {facts.filter(Boolean).map((fact) => (
              <span key={fact}>{fact}</span>
            ))}
            {media.watched && (
              <span className="detail-watched">
                <CheckCircleFilled /> {t('watched')}
              </span>
            )}
          </div>
          <div className="detail-genres">
            {media.genres.map((genre) => (
              <Tag key={genre}>{genre}</Tag>
            ))}
          </div>
        </div>
      </header>

      <div className="detail-body">{children}</div>
    </article>
  );
}

/** A titled block of the detail page; renders nothing without content. */
export function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="detail-section">
      <h2>{title}</h2>
      {children}
    </section>
  );
}

/** A two-column list of labelled values; empty values are left out. */
export function Facts({ rows }: { rows: [string, string | number | null | undefined][] }) {
  const filled = rows.filter(([, value]) => value !== null && value !== undefined && value !== '' && value !== 0);
  if (filled.length === 0) {
    return null;
  }
  return (
    <dl className="facts">
      {filled.map(([label, value]) => (
        <div key={label}>
          <dt>{label}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  );
}

/** A horizontally scrollable strip of persons. */
export function People({ persons }: { persons: Person[] }) {
  return (
    <ul className="people">
      {persons.map((person, index) => (
        <li key={`${person.name}-${index}`}>
          <span className="people-name">{person.name}</span>
          {person.role && <span className="people-role">{person.role}</span>}
        </li>
      ))}
    </ul>
  );
}

/** Formats an ISO date for the current locale. */
export function formatDate(iso: string | null): string {
  if (!iso) {
    return '';
  }
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleDateString();
}

/** Formats a file size in bytes. */
export function formatFileSize(bytes: number): string {
  if (!bytes || bytes <= 0) {
    return '';
  }
  const gigabytes = bytes / 1024 ** 3;
  return gigabytes >= 1 ? `${gigabytes.toFixed(2)} GB` : `${(bytes / 1024 ** 2).toFixed(0)} MB`;
}
