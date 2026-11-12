import { CheckCircleFilled, StarFilled } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import { ArtworkImage } from './ArtworkImage';

interface PosterCardProps {
  to: string;
  title: string;
  subtitle: string;
  posterPath: string | undefined;
  rating: number;
  watched: boolean;
}

/** A large, touch-friendly tile of the library grid. */
export function PosterCard({ to, title, subtitle, posterPath, rating, watched }: PosterCardProps) {
  return (
    <Link to={to} className="poster-card">
      <div className="poster-frame">
        <ArtworkImage path={posterPath} alt={title} className="poster" />
        {watched && <CheckCircleFilled className="poster-watched" />}
        {rating > 0 && (
          <span className="poster-rating">
            <StarFilled /> {rating.toFixed(1)}
          </span>
        )}
      </div>
      <div className="poster-title">{title}</div>
      <div className="poster-subtitle">{subtitle}</div>
    </Link>
  );
}
