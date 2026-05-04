interface ConfidenceBarProps {
  value: number;
  max?: number;
  label?: string;
}

export default function ConfidenceBar({
  value,
  max = 1,
  label,
}: ConfidenceBarProps) {
  const percentage = Math.min(100, Math.max(0, (value / max) * 100));
  const displayLabel = label ?? `Score: ${(percentage).toFixed(0)}%`;

  return (
    <div
      className="confidence-bar"
      role="progressbar"
      aria-valuenow={value}
      aria-valuemin={0}
      aria-valuemax={max}
      aria-label={displayLabel}
    >
      <div
        className="confidence-bar-fill"
        style={{ width: `${percentage}%` }}
      />
      <span className="confidence-bar-label">{displayLabel}</span>
    </div>
  );
}
