import type { MemoryStatus } from '../types';
import { LIFECYCLE_COLORS } from '../types';

interface LifecycleBadgeProps {
  status: MemoryStatus;
}

export default function LifecycleBadge({ status }: LifecycleBadgeProps) {
  const color = LIFECYCLE_COLORS[status] || '#6b7280';
  const label = status.charAt(0).toUpperCase() + status.slice(1);

  return (
    <span
      className="lifecycle-badge"
      style={{ backgroundColor: color }}
      aria-label={`Status: ${label}`}
    >
      {label}
    </span>
  );
}
