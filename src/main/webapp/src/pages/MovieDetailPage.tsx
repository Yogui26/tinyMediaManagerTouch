import { useParams } from 'react-router-dom';
import { ErrorView, Loading } from '../components/StateViews';
import { useAsync } from '../hooks/useAsync';
import { t } from '../i18n';
import { fetchMovie } from '../services/api';
import { DetailLayout, Facts, People, Section, formatDate, formatFileSize } from './DetailLayout';

/** All details of a single movie. */
export function MovieDetailPage() {
  const { id = '' } = useParams();
  const { data: movie, error, loading, reload } = useAsync((signal) => fetchMovie(id, signal), [id]);

  if (error !== undefined) {
    return <ErrorView error={error} onRetry={reload} />;
  }
  if (!movie || loading) {
    return <Loading />;
  }

  const info = movie.mediaInfo;
  const video = [info.videoFormat, info.videoCodec, info.hdrFormat].filter(Boolean).join(' · ');
  const audio = [info.audioCodec, info.audioChannels, info.audioLanguage].filter(Boolean).join(' · ');

  return (
    <DetailLayout
      basePath={`/v2/movies/${movie.id}`}
      parentRoute="/movies"
      media={movie}
      tagline={movie.tagline}
      facts={[movie.year > 0 ? String(movie.year) : '', movie.runtime > 0 ? t('minutes', { n: movie.runtime }) : '', movie.certification ?? '']}
    >
      {movie.plot && (
        <Section title={t('plot')}>
          <p className="plot">{movie.plot}</p>
        </Section>
      )}

      {movie.actors.length > 0 && (
        <Section title={t('cast')}>
          <People persons={movie.actors} />
        </Section>
      )}

      {(movie.directors.length > 0 || movie.writers.length > 0) && (
        <Section title={t('crew')}>
          <Facts
            rows={[
              [t('directors'), movie.directors.map((person) => person.name).join(', ')],
              [t('writers'), movie.writers.map((person) => person.name).join(', ')],
            ]}
          />
        </Section>
      )}

      <Section title={t('details')}>
        <Facts
          rows={[
            [t('releaseDate'), formatDate(movie.releaseDate)],
            [t('country'), movie.country],
            [t('languages'), movie.spokenLanguages],
            [t('studio'), movie.productionCompany],
            [t('movieSet'), movie.movieSet?.title],
            [t('tags'), movie.tags.join(', ')],
          ]}
        />
      </Section>

      <Section title={t('mediaInfo')}>
        <Facts
          rows={[
            [t('video'), video],
            [t('audio'), audio],
            [t('container'), info.container],
            [t('fileSize'), formatFileSize(info.fileSize)],
            [t('path'), movie.path],
          ]}
        />
      </Section>
    </DetailLayout>
  );
}
