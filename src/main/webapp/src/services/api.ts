import axios, { AxiosError } from 'axios';

const API_KEY_STORAGE = 'tmm.apiKey';

/** Reads the API key remembered in this browser. */
export function getApiKey(): string {
  try {
    return localStorage.getItem(API_KEY_STORAGE) ?? '';
  } catch {
    return '';
  }
}

/** Remembers (or forgets, when empty) the API key in this browser. */
export function setApiKey(apiKey: string): void {
  try {
    if (apiKey) {
      localStorage.setItem(API_KEY_STORAGE, apiKey);
    } else {
      localStorage.removeItem(API_KEY_STORAGE);
    }
  } catch {
    // storage not available - the key is only kept until the page is reloaded
  }
  memoryApiKey = apiKey;
}

let memoryApiKey = getApiKey();

/** The axios instance for all API calls; it injects the API key automatically. */
export const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use((config) => {
  if (memoryApiKey) {
    config.headers.set('api-key', memoryApiKey);
  }
  return config;
});

/** The envelope every JSON response of tinyMediaManager is wrapped in. */
interface Envelope<T> {
  message: string;
  data: T;
}

export interface Page<T> {
  total: number;
  offset: number;
  limit: number;
  items: T[];
}

export interface Person {
  name: string;
  role: string;
}

export interface Rating {
  id: string;
  rating: number;
  votes: number;
  maxValue: number;
}

export interface MediaSummary {
  id: string;
  title: string;
  originalTitle: string;
  year: number;
  rating: number;
  watched: boolean;
  genres: string[];
  certification: string | null;
  dateAdded: string | null;
  hasPoster: boolean;
  hasFanart: boolean;
}

export interface MovieSummary extends MediaSummary {
  runtime: number;
  videoFormat: string;
}

export interface MovieDetail extends MovieSummary {
  tagline: string;
  plot: string;
  releaseDate: string | null;
  country: string;
  spokenLanguages: string;
  productionCompany: string;
  top250: number;
  imdbId: string;
  tmdbId: number;
  tags: string[];
  ratings: Rating[];
  actors: Person[];
  directors: Person[];
  writers: Person[];
  movieSet?: { id: string; title: string };
  mediaInfo: {
    container: string;
    videoFormat: string;
    videoCodec: string;
    videoResolution: string;
    hdrFormat: string;
    audioCodec: string;
    audioChannels: string;
    audioLanguage: string;
    fileSize: number;
  };
  dataSource: string;
  path: string | null;
}

export interface TvShowSummary extends MediaSummary {
  status: string | null;
  seasonCount: number;
  episodeCount: number;
}

export interface Season {
  season: number;
  title: string;
  episodeCount: number;
  watched: boolean;
  hasPoster: boolean;
}

export interface TvShowDetail extends TvShowSummary {
  plot: string;
  firstAired: string | null;
  runtime: number;
  country: string;
  productionCompany: string;
  tags: string[];
  ratings: Rating[];
  actors: Person[];
  dataSource: string;
  path: string | null;
  seasons: Season[];
}

export interface Episode {
  id: string;
  season: number;
  episode: number;
  title: string;
  plot: string;
  firstAired: string | null;
  rating: number;
  runtime: number;
  watched: boolean;
  videoFormat: string;
}

export interface SystemStatus {
  version: string;
  state: string;
  busy: boolean;
}

export type SortField = 'title' | 'year' | 'rating' | 'dateAdded';

export interface ListQuery {
  q?: string;
  sort?: SortField;
  order?: 'asc' | 'desc';
  watched?: boolean;
  offset?: number;
  limit?: number;
}

async function get<T>(url: string, params?: object, signal?: AbortSignal): Promise<T> {
  const response = await api.get<Envelope<T>>(url, { params, signal });
  return response.data.data;
}

export const fetchStatus = (signal?: AbortSignal) => get<SystemStatus>('/system', undefined, signal);

export const fetchMovies = (query: ListQuery, signal?: AbortSignal) => get<Page<MovieSummary>>('/v2/movies', query, signal);

export const fetchMovie = (id: string, signal?: AbortSignal) => get<MovieDetail>(`/v2/movies/${id}`, undefined, signal);

export const fetchTvShows = (query: ListQuery, signal?: AbortSignal) => get<Page<TvShowSummary>>('/v2/tvshows', query, signal);

export const fetchTvShow = (id: string, signal?: AbortSignal) => get<TvShowDetail>(`/v2/tvshows/${id}`, undefined, signal);

export const fetchEpisodes = (id: string, season: number, signal?: AbortSignal) =>
  get<Episode[]>(`/v2/tvshows/${id}/seasons/${season}/episodes`, undefined, signal);

/** Loads an artwork with the API key and returns it as an image blob. */
export async function fetchArtwork(path: string, signal?: AbortSignal): Promise<Blob> {
  const response = await api.get<Blob>(path, { responseType: 'blob', signal });
  return response.data;
}

/** True if the request has been cancelled on purpose (e.g. because the view has been left). */
export function isCancel(error: unknown): boolean {
  return axios.isCancel(error);
}

/** True if the server rejected the API key. */
export function isUnauthorized(error: unknown): boolean {
  return error instanceof AxiosError && error.response?.status === 403;
}

/** Extracts a user-friendly message out of any error. */
export function errorMessage(error: unknown): string {
  if (error instanceof AxiosError) {
    const message = (error.response?.data as Partial<Envelope<unknown>> | undefined)?.message;
    if (typeof message === 'string' && message) {
      return message;
    }
    if (!error.response) {
      return 'tinyMediaManager is not reachable';
    }
    return `Request failed (${error.response.status})`;
  }
  return error instanceof Error ? error.message : String(error);
}

/** The name of the window event fired whenever the server rejects the API key. */
export const UNAUTHORIZED_EVENT = 'tmm:unauthorized';

api.interceptors.response.use(undefined, (error: unknown) => {
  if (isUnauthorized(error)) {
    window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
  }
  return Promise.reject(error);
});
