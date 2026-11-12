import { SearchOutlined, SortAscendingOutlined, SortDescendingOutlined } from '@ant-design/icons';
import { Button, Empty, Input, Segmented, Select } from 'antd';
import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { PosterCard } from '../components/PosterCard';
import { ErrorView, Loading } from '../components/StateViews';
import { t } from '../i18n';
import { fetchMovies, fetchTvShows, isCancel } from '../services/api';
import type { ListQuery, MediaSummary, MovieSummary, Page, SortField, TvShowSummary } from '../services/api';

const PAGE_SIZE = 60;

type Kind = 'movies' | 'tvshows';
type WatchedFilter = 'all' | 'watched' | 'unwatched';

interface Filters {
  q: string;
  sort: SortField;
  order: 'asc' | 'desc';
  watched: WatchedFilter;
}

interface Snapshot {
  filters: Filters;
  items: MediaSummary[];
  total: number;
  scrollY: number;
}

const DEFAULT_FILTERS: Filters = { q: '', sort: 'title', order: 'asc', watched: 'all' };

// remembers the list while a detail page is open, so "back" returns to the same position
const snapshots = new Map<Kind, Snapshot>();

function subtitleOf(kind: Kind, item: MediaSummary): string {
  const year = item.year > 0 ? String(item.year) : '';
  if (kind === 'tvshows') {
    const episodes = t('episodes', { n: (item as TvShowSummary).episodeCount });
    return year ? `${year} · ${episodes}` : episodes;
  }
  const runtime = (item as MovieSummary).runtime;
  const minutes = runtime > 0 ? t('minutes', { n: runtime }) : '';
  return [year, minutes].filter(Boolean).join(' · ');
}

function load(kind: Kind, query: ListQuery, signal: AbortSignal): Promise<Page<MediaSummary>> {
  return kind === 'movies' ? fetchMovies(query, signal) : fetchTvShows(query, signal);
}

/** The poster grid of all movies or TV shows with search, sorting and a watched filter. */
export function LibraryPage({ kind }: { kind: Kind }) {
  const snapshot = snapshots.get(kind);
  const [filters, setFilters] = useState<Filters>(snapshot?.filters ?? DEFAULT_FILTERS);
  const [searchText, setSearchText] = useState(filters.q);
  const [items, setItems] = useState<MediaSummary[]>(snapshot?.items ?? []);
  const [total, setTotal] = useState(snapshot?.total ?? 0);
  const [loading, setLoading] = useState(!snapshot);
  const [error, setError] = useState<unknown>();
  const [attempt, setAttempt] = useState(0);
  const [offset, setOffset] = useState(0);
  const skipInitialLoad = useRef(Boolean(snapshot));
  const sentinel = useRef<HTMLDivElement>(null);

  // restore the scroll position when coming back from a detail page
  useLayoutEffect(() => {
    if (snapshot) {
      window.scrollTo(0, snapshot.scrollY);
    }
    // only on mount
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // keep the snapshot up to date
  useEffect(() => {
    const save = () => snapshots.set(kind, { filters, items, total, scrollY: window.scrollY });
    save();
    window.addEventListener('scroll', save, { passive: true });
    return () => window.removeEventListener('scroll', save);
  }, [kind, filters, items, total]);

  // debounce typing in the search field
  useEffect(() => {
    const timer = window.setTimeout(() => {
      setFilters((previous) => (previous.q === searchText.trim() ? previous : { ...previous, q: searchText.trim() }));
    }, 300);
    return () => window.clearTimeout(timer);
  }, [searchText]);

  const changeFilters = useCallback((change: Partial<Filters>) => {
    setFilters((previous) => ({ ...previous, ...change }));
  }, []);

  // a changed filter starts again at the first page
  const [loadedFilters, setLoadedFilters] = useState(filters);
  if (loadedFilters !== filters) {
    setLoadedFilters(filters);
    setOffset(0);
  }

  useEffect(() => {
    if (skipInitialLoad.current) {
      skipInitialLoad.current = false;
      return;
    }

    const controller = new AbortController();
    setLoading(true);
    setError(undefined);

    const query: ListQuery = {
      q: filters.q || undefined,
      sort: filters.sort,
      order: filters.order,
      watched: filters.watched === 'all' ? undefined : filters.watched === 'watched',
      offset,
      limit: PAGE_SIZE,
    };

    load(kind, query, controller.signal)
      .then((page) => {
        setItems((previous) => (offset === 0 ? page.items : [...previous.slice(0, offset), ...page.items]));
        setTotal(page.total);
        setLoading(false);
      })
      .catch((reason: unknown) => {
        if (!isCancel(reason)) {
          setError(reason);
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [kind, filters, offset, attempt]);

  const hasMore = items.length < total;
  const loadMore = useCallback(() => {
    if (!loading && hasMore) {
      setOffset(items.length);
    }
  }, [loading, hasMore, items.length]);

  // load the next page automatically when the end of the list is reached
  useEffect(() => {
    const element = sentinel.current;
    if (!element) {
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          loadMore();
        }
      },
      { rootMargin: '600px' },
    );
    observer.observe(element);
    return () => observer.disconnect();
  }, [loadMore]);

  return (
    <div className="library">
      <div className="toolbar">
        <Input
          className="toolbar-search"
          size="large"
          allowClear
          inputMode="search"
          enterKeyHint="search"
          prefix={<SearchOutlined />}
          placeholder={t('search')}
          aria-label={t('search')}
          value={searchText}
          onChange={(event) => setSearchText(event.target.value)}
        />
        <div className="toolbar-row">
          <Select<SortField>
            size="large"
            className="toolbar-sort"
            aria-label={t('sortBy')}
            value={filters.sort}
            onChange={(sort) => changeFilters({ sort, order: sort === 'title' ? 'asc' : 'desc' })}
            options={[
              { value: 'title', label: t('sortTitle') },
              { value: 'year', label: t('sortYear') },
              { value: 'rating', label: t('sortRating') },
              { value: 'dateAdded', label: t('sortDateAdded') },
            ]}
          />
          <Button
            size="large"
            aria-label={filters.order === 'asc' ? t('ascending') : t('descending')}
            icon={filters.order === 'asc' ? <SortAscendingOutlined /> : <SortDescendingOutlined />}
            onClick={() => changeFilters({ order: filters.order === 'asc' ? 'desc' : 'asc' })}
          />
          <Segmented<WatchedFilter>
            size="large"
            value={filters.watched}
            onChange={(watched) => changeFilters({ watched })}
            options={[
              { value: 'all', label: t('all') },
              { value: 'watched', label: t('watched') },
              { value: 'unwatched', label: t('unwatched') },
            ]}
          />
        </div>
      </div>

      {error !== undefined && items.length === 0 ? (
        <ErrorView error={error} onRetry={() => setAttempt((value) => value + 1)} />
      ) : items.length === 0 && loading ? (
        <Loading />
      ) : items.length === 0 ? (
        <Empty className="centered" description={t('nothingFound')} />
      ) : (
        <>
          <div className="grid">
            {items.map((item) => (
              <PosterCard
                key={item.id}
                to={`/${kind}/${item.id}`}
                title={item.title}
                subtitle={subtitleOf(kind, item)}
                posterPath={item.hasPoster ? `/v2/${kind}/${item.id}/artwork/poster` : undefined}
                rating={item.rating}
                watched={item.watched}
              />
            ))}
          </div>
          <div className="list-footer" ref={sentinel}>
            <span>{t('countOf', { shown: items.length, total })}</span>
            {hasMore && (
              <Button size="large" loading={loading} onClick={error !== undefined ? () => setAttempt((value) => value + 1) : loadMore}>
                {error !== undefined ? t('retry') : t('loadMore')}
              </Button>
            )}
          </div>
        </>
      )}
    </div>
  );
}
