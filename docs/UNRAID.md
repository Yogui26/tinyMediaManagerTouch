# Web UI container on Unraid (experimental)

The image `ghcr.io/yogui26/tinymediamanagertouch` runs tinyMediaManager without the desktop GUI: it only serves the
HTTP API and the web UI on port 7878. It is built by `.github/workflows/docker.yml` from `docker/web/`.

## Install

1. Make the image pullable: on GitHub open the package `tinymediamanagertouch` → *Package settings* → *Change
   visibility* → *Public* (or run `docker login ghcr.io` in the Unraid terminal with a token that has `read:packages`).
2. In the Unraid terminal, download the template:

   ```
   wget -O /boot/config/plugins/dockerMan/templates-user/my-tinyMediaManagerTouch.xml \
     https://raw.githubusercontent.com/Yogui26/tinyMediaManagerTouch/feature/touch-web-ui/unraid/tinyMediaManagerTouch.xml
   ```

3. *Docker* → *Add Container* → template `tinyMediaManagerTouch`, then fill in:
   - **API key**: a long random value; the web UI asks for it.
   - **Movies** / **TV shows**: your media folders. They are mapped read-only by default.
4. *Apply*, then open the web UI (`http://<server>:7878/`) and enter the API key.
5. *Settings* → *Update library*. The movies and TV shows show up once the task has finished.

## Configuration

| Variable                 | Purpose                                                             |
|:-------------------------|:--------------------------------------------------------------------|
| `TMM_API_KEY`            | enables the HTTP API and sets its key (required on the first start) |
| `TMM_API_PORT`           | port inside the container (default 7878)                            |
| `TMM_MOVIE_DATASOURCES`  | movie folders inside the container, separated by `;`                |
| `TMM_TVSHOW_DATASOURCES` | TV show folders inside the container, separated by `;`              |
| `JAVA_OPTS`              | JVM options (default `-Xms64m -Xmx1g`)                              |

Everything tinyMediaManager stores (settings, database, image cache, logs) lives in `/data`.

## Notes

- With read-only media folders the library can be browsed, but scraping cannot write NFO files or artwork. Switch
  the path mode to read/write once you want tinyMediaManager to manage the files.
- All other settings (scrapers, renamer, …) are not available in the web UI yet; they keep their defaults or can be
  edited in the JSON files in `/data/data` while the container is stopped.
- The connection is plain HTTP. Do not expose the port to the internet; use a VPN or a reverse proxy with TLS.
- Logs: `/data/logs` and the Unraid container log.
