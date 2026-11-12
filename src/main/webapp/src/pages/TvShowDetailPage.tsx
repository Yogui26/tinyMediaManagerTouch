import { CheckCircleFilled } from '@ant-design/icons';
import { Collapse } from 'antd';
import { useParams } from 'react-router-dom';
import { ErrorView, Loading } from '../components/StateViews';
import { useAsync } from '../hooks/useAsync';
import { t } from '../i18n';
import { fetchEpisodes, fetchTvShow } from '../services/api';
import type { Season } from '../services/api';
import { DetailLayout, Facts, People, Section, formatDate } from './DetailLayout';

function seasonTitle(season: Season): string {
  if (season.title) {
    return season.title;
  }
  return season.season === 0 ? t('specials') : t('season', { n: season.season });
}

/** The episodes of one season; only loaded once the season is opened. */
function EpisodeList({ tvShowId, season }: { tvShowId: string; season: number }) {
  const { data: episodes, error, loading, reload } = useAsync((signal) => fetchEpisodes(tvShowId, season, signal), [tvShowId, season]);

  if (error !== undefined) {
    return <ErrorView error={error} onRetry={reload} />;
  }
  if (!episodes || loading) {
    return <Loading />;
  }

  return (
    <ol className="episodes">
      {episodes.map((episode) => (
        <li key={episode.id}>
          <span className="episode-number">{episode.episode}</span>
          <div className="episode-text">
            <div className="episode-title">
              {episode.title}
              {episode.watched && <CheckCircleFilled className="episode-watched" aria-label={t('watched')} />}
            </div>
            <div className="episode-meta">
              {[formatDate(episode.firstAired), episode.runtime > 0 ? t('minutes', { n: episode.runtime }) : '', episode.videoFormat]
                .filter(Boolean)
                .join(' · ')}
            </div>
            {episode.plot && <p className="episode-plot">{episode.plot}</p>}
          </div>
        </li>
      ))}
    </ol>
  );
}

/** All details of a single TV show including its seasons and episodes. */
export function TvShowDetailPage() {
  const { id = '' } = useParams();
  const { data: tvShow, error, loading, reload } = useAsync((signal) => fetchTvShow(id, signal), [id]);

  if (error !== undefined) {
    return <ErrorView error={error} onRetry={reload} />;
  }
  if (!tvShow || loading) {
    return <Loading />;
  }

  return (
    <DetailLayout
      basePath={`/v2/tvshows/${tvShow.id}`}
      parentRoute="/tvshows"
      media={tvShow}
      facts={[tvShow.year > 0 ? String(tvShow.year) : '', t('episodes', { n: tvShow.episodeCount }), tvShow.certification ?? '']}
    >
      {tvShow.plot && (
        <Section title={t('plot')}>
          <p className="plot">{tvShow.plot}</p>
        </Section>
      )}

      {tvShow.seasons.length > 0 && (
        <Section title={t('seasons')}>
          <Collapse
            accordion
            destroyOnHidden
            size="large"
            items={tvShow.seasons.map((season) => ({
              key: String(season.season),
              label: seasonTitle(season),
              extra: <span className="season-count">{t('episodes', { n: season.episodeCount })}</span>,
              children: <EpisodeList tvShowId={tvShow.id} season={season.season} />,
            }))}
          />
        </Section>
      )}

      {tvShow.actors.length > 0 && (
        <Section title={t('cast')}>
          <People persons={tvShow.actors} />
        </Section>
      )}

      <Section title={t('details')}>
        <Facts
          rows={[
            [t('firstAired'), formatDate(tvShow.firstAired)],
            [t('status'), tvShow.status],
            [t('country'), tvShow.country],
            [t('studio'), tvShow.productionCompany],
            [t('tags'), tvShow.tags.join(', ')],
            [t('path'), tvShow.path],
          ]}
        />
      </Section>
    </DetailLayout>
  );
}
