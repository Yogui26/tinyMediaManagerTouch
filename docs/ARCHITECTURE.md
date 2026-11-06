# System Architecture

## 1. Architecture Overview

tinyMediaManager is a **monolithic, module-oriented desktop application** (Java/Swing) for Windows, Linux, and macOS.
There is no server backend and no microservices: a single JVM application serves the GUI, the CLI, and a local HTTP
control server through the same core components.

The dominant architectural pattern is a **layered and modular architecture with event-driven communication**:

- **Presentation layer (`ui`)** – a pure Swing view/controller layer; it contains no business logic and communicates
  with the core exclusively through module managers, task classes, and property-change listeners.
- **Application/module layer (`core.movie`, `core.tvshow`, `*ModuleManager`, `core.tasks`)** – orchestrates the
  workflows (update, scrape, rename, export, …). `TmmModuleManager` registers modules lazily via the `ITmmModule`
  interface; movie and TV show logic are strictly separated.
- **Domain layer (`core.entities`, `core.movie.entities`, `core.tvshow.entities`)** – entities (`Movie`, `MovieSet`,
  `TvShow`, `TvShowSeason`, `TvShowEpisode`, `MediaFile`) based on `MediaEntity`/`AbstractModelObject` with
  `PropertyChangeSupport` for observable behavior.
- **Integration/provider layer (`scraper`, `thirdparty`, `addon`)** – encapsulates external knowledge and tools behind
  registries: `MediaProviders` is the registry for about 25 metadata, subtitle, and trailer providers implementing
  `IMovieMetadataProvider` et al. (`scraper.interfaces`); `IAddon`/`IAddonProvider` (SPI) covers add-ons (FFmpeg,
  FFprobe, yt-dlp, Deno).

**Decoupling mechanisms:**

- **Registries instead of direct references:** `MediaProviders`, `TmmModuleManager`, and `ExternalTools` wire components
  together at runtime through interfaces.
- **Event bus:** `core.bus.EventBus` (pub/sub via `IEventListener`) plus `MessageManager` for user feedback decouple the
  core from the UI.
- **Task pattern:** background work runs exclusively in `TmmTask` objects on `TmmThreadPool`/`TmmTaskManager`; UI
  updates happen only on the EDT, triggered by event/property callbacks.
- **Persistence decoupling:** entities are serialized (Jackson) into `data/`, internal state is kept in an MVStore
  key/value store (`TmmStore`); NFO connectors (`core.*.connector`) write foreign formats
  (Kodi/Emby/Jellyfin/MediaPortal) without making them the master data source.
- **Multiple entry points, one core:** `TinyMediaManager` (GUI), `cli.TinyMediaManagerCLI` (picocli), and
  `core.http.TmmHttpServer` with `ITmmCommandHandler` (remote control/web UI) invoke identical module APIs.

## 2. Technology Stack

| Component                  | Technology                                                                                                                                      | Purpose                                                             |
|:---------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------|:--------------------------------------------------------------------|
| Language/Runtimes          | Java 17 (`maven.compiler.release`), bundled JRE 25 (macOS 21)                                                                                   | Desktop app for Windows/Linux/macOS, x64 + aarch64                  |
| Build                      | Maven 3.9+ (`mvnw`), shade, assembly, and failsafe plugins                                                                                      | Fat-app distribution with bundled JRE, GitLab CI (`.gitlab-ci.yml`) |
| UI toolkit                 | Swing, FlatLaf, MigLayout, svgSalamander                                                                                                        | Cross-platform look and feel, SVG icons                             |
| UI binding/lists           | Glazed Lists, BetterBeansBinding                                                                                                                | Tables, filtering/sorting, property-change binding                  |
| Serialization              | Jackson (databind, YAML, blackbird)                                                                                                             | Entity and settings persistence (JSON/YAML)                         |
| Local key/value store      | H2 MVStore (`h2-mvstore`)                                                                                                                       | `TmmStore`: flags, license/update state, caches                     |
| Metadata format            | Custom NFO connectors (Kodi/Emby/Jellyfin/MediaPortal)                                                                                          | Sidecar files in foreign formats                                    |
| HTTP client                | OkHttp (+ brotli, logging), Retrofit/Gson converter, jsoup                                                                                      | Scraper/API calls, HTML scraping                                    |
| Metadata providers         | trakt-java, kodi-json-rpc, tmdb, imdb, thetvdb, fanarttv, omdb, ofdb, moviemeter, mdblist, tvmaze, anidb, thesportsdb, fernsehserien, mpdbtv, … | `MediaProviders` registry                                           |
| Subtitle/trailer search    | opensubtitles(+com), subdl, yifysubtitles, hdtrailersnet, ffmpeg/yt-dlp                                                                         | Enrichment of media data                                            |
| Browser automation         | Selenium Chrome driver                                                                                                                          | Fallback for complex web sources                                    |
| Image processing           | imgscalr, twelvemonkeys imageio (JPEG/WebP), animated-gif-lib                                                                                   | Artwork resizing and caching                                        |
| Media tooling              | MediaInfo (JNA native), FFmpeg/FFprobe (add-ons), mp4parser, loop-fs-iso/udf, jUPnP                                                             | Technical analysis, ISO mounting, UPnP                              |
| Export engine              | jmte (template engine, extended in `core.jmte`)                                                                                                 | HTML/CSV export from `templates/`                                   |
| Infrastructure             | picocli (CLI), aXMLRPC, getdown-core + custom updater                                                                                           | CLI parsing, remote-control protocol, auto-update                   |
| Logging                    | SLF4J + Logback (+ JCL/JUL bridge)                                                                                                              | Diagnostics, in-UI log collector                                    |
| Tests                      | JUnit 4, AssertJ, MockWebServer; surefire/failsafe                                                                                              | Unit and integration tests (skipped by default)                     |
| Quality/compliance tooling | SonarQube, OWASP dependency-check, CycloneDX SBOM, forbiddenapis, modernizer                                                                    | Code quality, SCA, SBOM                                             |
| Internationalization       | About 50 `messages_*.properties` files                                                                                                          | User-facing translations                                            |

## 3. Repository Layout

**First level (project root):**

| Directory/File                                                                   | Responsibility                                                                        |
|:---------------------------------------------------------------------------------|:--------------------------------------------------------------------------------------|
| `pom.xml`, `mvnw`                                                                | Maven build including shade/assembly/CI profiles                                      |
| `src/`                                                                           | Source code and build descriptors (see second level)                                  |
| `docs/`                                                                          | Developer documentation (architecture, Java conventions, testing)                     |
| `addons/`                                                                        | External add-on binaries (FFmpeg, FFprobe, yt-dlp, Deno) used at runtime              |
| `plugins/`                                                                       | Optional SPI plugin JARs (example: `spi-template`)                                    |
| `templates/`                                                                     | Export and rename templates                                                           |
| `native/`, `AppBundler/`, `build/`                                               | Platform bundling (launchers, macOS bundler, build helpers)                           |
| `data/`, `cache/`, `logs/`, `backup/`, `update/`                                 | Runtime artifacts: settings/entities, image cache, logs, backup, Getdown update files |
| `.gitlab-ci.yml`, `ant-getdown.xml`, `external-tools.json`, `launcher-extra.yml` | CI pipeline, Getdown update, external tool definitions                                |
| `target/`, `dbmigrator.jar`                                                      | Maven output, helper tool for legacy database imports                                 |

**Second level (`src/main/java/org/tinymediamanager/`):**

| Package                                                          | Responsibility                                                                                                                                           |
|:-----------------------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------|
| `core`                                                           | Shared core: entity base (`MediaEntity`, `MediaFile`), utilities, settings, image cache, exporters, `ITmmModule` interface, `EventBus`, `MessageManager` |
| `core.bus`                                                       | In-process pub/sub event system                                                                                                                          |
| `core.entities`                                                  | Base element types such as `MediaFile`, `MediaGenres`, `Person`                                                                                          |
| `core.tasks`                                                     | Task classes (cleanup, image cache, downloads, AR detector, export)                                                                                      |
| `core.threading`                                                 | `TmmThreadPool`, `TmmTaskManager`, `TmmTask` – background processing                                                                                     |
| `core.http`                                                      | `TmmHttpServer` + `ITmmCommandHandler` – local command interface for CLI/web                                                                             |
| `core.jmte`                                                      | Template engine extensions (renderers, encoders) for exports                                                                                             |
| `core.mediainfo`                                                 | Wrapper around MediaInfo for technical probing                                                                                                           |
| `core.movie`                                                     | Movie domain: entities, settings, `MovieList`, renamer, `MovieModuleManager`, upgrade tasks                                                              |
| `core.movie.connector`                                           | NFO output formats (Kodi/Emby/Jellyfin/MediaPortal)                                                                                                      |
| `core.movie.filenaming`, `core.movie.jmte`, `core.movie.tasks`   | File naming, export renderers, movie tasks                                                                                                               |
| `core.tvshow` (+ `entities`, `tasks`, `filenaming`, `connector`) | TV show domain, analogous to the movie domain                                                                                                            |
| `scraper`                                                        | Metadata/artwork/subtitle providers: registry (`MediaProviders`), data models (`MediaMetadata`, `MediaSearchResult`), about 25 provider packages         |
| `scraper.spi`                                                    | Add-on/plugin SPI binding point (`IAddonProvider`)                                                                                                       |
| `ui`                                                             | Swing presentation: `MainWindow`, movie/TV show views (`movies`, `tvshows`, `moviesets`), settings, dialogs, wizard, components                          |
| `thirdparty`                                                     | Native/external tools: FFmpeg, MediaInfo, KodiRPC, UPnP, yt-dlp, file dialogs                                                                            |
| `updater`                                                        | Update check and installation (Getdown, macOS app updater)                                                                                               |
| `cli`                                                            | Headless entry point via picocli (movie/TV show subcommands)                                                                                             |
| `logging`                                                        | Logback configuration backstop/log backstop                                                                                                              |
| `addon`                                                          | `IAddon` implementations (FFmpeg, FFprobe, yt-dlp, Deno) – binaries from `addons/`                                                                       |
| Root classes                                                     | `TinyMediaManager` (bootstrap), `Globals`, `UpgradeTasks` (global migration routine)                                                                     |

**Other `src/` folders:** `src/test` (unit tests, samples), `src/integration-test` (ITs via failsafe), `src/assembly`
(packaging descriptors), `src/docker` (container templates), `src/main/webapp` (currently only `node_modules` of an
in-development React/Vite web UI; no production code is stored in the repository).

## 4. Data Flow & Component Interaction

Typical flow (user clicks "Update Data Sources" for movies in the GUI):

1. **Entry (presentation layer):** The click in the view under `ui/movies` calls the `MovieModuleManager`. The UI does
   not block; it registers itself as a listener (`MessageManager`, property callbacks).
2. **Task delegation (module/threading layer):** The module manager builds a `TmmTask` object (e.g.,
   `MovieUpdateDatasourceTask`) and hands it to `TmmTaskManager` → `TmmThreadPool`. Progress events flow back to the
   status bar via `TmmTaskHandle`.
3. **File system detection (integration section):** `FilesFirstWalker` walks the configured data sources
   (`core.DataSourceUtils`), identifies video files, calls `core.mediainfo` or native `thirdparty/MediaInfo` (JNA) for
   technical details, and creates `MediaFile` structures including `MediaSource`/`MediaStreamInfo`.
4. **Domain update:** `Movie` objects in `MovieList` are added or updated. All entities inherit the observable
   infrastructure (`AbstractModelObject`), fire `PropertyChangeEvents`, and are announced via `EventBus` events.
5. **Persistence:** Changed entities are serialized into `data/` via Jackson utilities; internal state lives in
   `TmmStore` (MVStore). Migration adjustments run through `MovieUpgradeTasks`/`UpgradeTasks`; legacy imports use
   `dbmigrator.jar`.
6. **Scraper access (when triggered):** `MovieScraperMetadataConfig` leads to `MovieSearchAndScrapeOptions` → the
   `MediaProviders` registry supplies the matching provider (e.g., `scraper.tmdb`). An OkHttp/Retrofit call runs
   underneath according to `MediaProviderInfo`; the result returns as `MediaSearchResult`/`MediaMetadata` and is mapped
   into the entity. Caching and rate limiting stay in the provider-specific HTTP helper classes (`scraper.http`).
7. **Output to media libraries:** NFO connectors write Kodi/Emby/Jellyfin-compatible files, artwork passes through the
   `ImageCache`, and renamer tasks (`core.movie.filenaming`, `MovieRenamer`, profiles/preview) generate target paths.
   The `PostProcessExecutor` allows post-processing; `thirdparty/FFmpeg` or add-ons from `addons/` may be used for this.
8. **UI feedback:** Every step fires events or property changes; views update exclusively on the EDT. Log and message
   collectors (`ui/TmmUILogCollector`, `MessageManager`) populate the log panel and message dialogs.
9. **Side calls with the same core:** The CLI entry point (`cli.TinyMediaManagerCLI` via picocli) and the local
   `TmmHttpServer` (`core.http`, `ITmmCommandHandler`) call the same module APIs. Protocol commands such as "scrape" or
   "update" end in identical task classes; the web frontend is connected through this interface.
10. **Infrastructure cross-relations:** `updater/UpdateCheck` checks Getdown channels for new versions, `License` gates
    features, and `ExternalTools` validates external binaries from `external-tools.json` and `addons/`. All state and
    configuration values are stored centrally in `TmmStore`.
