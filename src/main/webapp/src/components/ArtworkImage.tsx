import { PictureOutlined } from '@ant-design/icons';
import { useEffect, useRef, useState } from 'react';
import { fetchArtwork } from '../services/api';

interface ArtworkImageProps {
  /** the API path of the artwork (without the /api prefix), or undefined if there is none */
  path: string | undefined;
  alt: string;
  className?: string;
}

/**
 * Shows an artwork from the API. The image is only requested once it gets close to the viewport, because the API key
 * has to be sent as a header and therefore the browser cannot lazy-load it on its own.
 */
export function ArtworkImage({ path, alt, className }: ArtworkImageProps) {
  const ref = useRef<HTMLDivElement>(null);
  const [visible, setVisible] = useState(false);
  const [loaded, setLoaded] = useState<{ path: string; url: string }>();

  useEffect(() => {
    const element = ref.current;
    if (!element || visible) {
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          setVisible(true);
        }
      },
      { rootMargin: '300px' },
    );
    observer.observe(element);
    return () => observer.disconnect();
  }, [visible]);

  useEffect(() => {
    if (!path || !visible) {
      return;
    }
    const controller = new AbortController();
    let objectUrl: string | undefined;

    fetchArtwork(path, controller.signal)
      .then((blob) => {
        objectUrl = URL.createObjectURL(blob);
        setLoaded({ path, url: objectUrl });
      })
      .catch(() => {
        // keep the placeholder
      });

    return () => {
      controller.abort();
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [path, visible]);

  const url = path && loaded?.path === path ? loaded.url : undefined;

  return (
    <div ref={ref} className={`artwork ${className ?? ''}`}>
      {url ? <img src={url} alt={alt} draggable={false} /> : <PictureOutlined className="artwork-placeholder" aria-label={alt} />}
    </div>
  );
}
