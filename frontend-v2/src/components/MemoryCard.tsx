import type { MemoryFragment } from '../types';
import KindBadge from './KindBadge';
import LifecycleBadge from './LifecycleBadge';
import ConfidenceBar from './ConfidenceBar';

interface MemoryCardProps {
  memory: MemoryFragment;
  score?: number;
  onClick: (id: string) => void;
}

export default function MemoryCard({
  memory,
  score,
  onClick,
}: MemoryCardProps) {
  const snippet =
    memory.summary ||
    memory.content?.substring(0, 180) ||
    '(no content)';

  return (
    <article
      className="memory-card"
      onClick={() => onClick(memory.id)}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onClick(memory.id);
        }
      }}
      tabIndex={0}
      role="button"
      aria-label={`Memory: ${memory.summary || memory.id}`}
    >
      <div className="memory-card-header">
        <KindBadge kind={memory.kind} />
        <LifecycleBadge status={memory.lifecycle.status} />
        {score !== undefined && (
          <ConfidenceBar value={score} label={`Score: ${(score * 100).toFixed(0)}%`} />
        )}
      </div>

      <div className="memory-card-body">
        <p className="memory-card-snippet">{snippet}</p>
        {memory.topics.length > 0 && (
          <div className="memory-card-topics">
            {memory.topics.map((t) => (
              <span key={t} className="topic-chip">
                {t}
              </span>
            ))}
          </div>
        )}
      </div>

      <div className="memory-card-footer">
        <span className="memory-card-id" title={memory.id}>
          {memory.id}
        </span>
        <span className="memory-card-date">
          {new Date(memory.createdAt || memory.updatedAt).toLocaleDateString()}
        </span>
        {memory.lifecycle.sensitivity !== 'public' &&
          memory.lifecycle.sensitivity !== 'internal' && (
            <span className="sensitivity-tag">
              {memory.lifecycle.sensitivity}
            </span>
          )}
      </div>
    </article>
  );
}
