import type { MemoryKind } from '../types';
import { KIND_COLORS, KIND_LABELS } from '../types';

interface KindBadgeProps {
  kind: MemoryKind;
}

export default function KindBadge({ kind }: KindBadgeProps) {
  const color = KIND_COLORS[kind] || '#a3a3a3';
  const label = KIND_LABELS[kind] || kind;

  return (
    <span
      className="kind-badge"
      style={{ backgroundColor: color }}
      aria-label={`Kind: ${label}`}
    >
      {label}
    </span>
  );
}
