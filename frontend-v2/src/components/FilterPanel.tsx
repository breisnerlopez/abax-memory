import type { MemoryKind, MemoryStatus, SensitivityLevel } from '../types';
import { KIND_LABELS } from '../types';

export interface FilterValues {
  kinds: MemoryKind[];
  lifecycle: MemoryStatus | '';
  sensitivity: SensitivityLevel | '';
  createdAfter: string;
  createdBefore: string;
}

interface FilterPanelProps {
  values: FilterValues;
  onChange: (values: FilterValues) => void;
  collapsed?: boolean;
  onToggle?: () => void;
}

const ALL_KINDS: MemoryKind[] = [
  'fact',
  'preference',
  'event',
  'decision',
  'task',
  'procedure',
  'note',
  'entity',
];

const ALL_STATUSES: MemoryStatus[] = [
  'draft',
  'pending',
  'active',
  'archived',
  'rejected',
  'deleted',
];

const ALL_SENSITIVITIES: SensitivityLevel[] = [
  'public',
  'internal',
  'confidential',
  'secret',
];

export default function FilterPanel({
  values,
  onChange,
  collapsed = false,
  onToggle,
}: FilterPanelProps) {
  const toggleKind = (kind: MemoryKind) => {
    const kinds = values.kinds.includes(kind)
      ? values.kinds.filter((k) => k !== kind)
      : [...values.kinds, kind];
    onChange({ ...values, kinds });
  };

  const setField = <K extends keyof FilterValues>(
    field: K,
    val: FilterValues[K]
  ) => {
    onChange({ ...values, [field]: val });
  };

  if (collapsed) {
    return (
      <div className="filter-panel filter-panel-collapsed">
        <button
          className="filter-toggle"
          onClick={onToggle}
          aria-expanded="false"
        >
          &#9776; Filters
        </button>
      </div>
    );
  }

  return (
    <aside className="filter-panel" aria-label="Search filters">
      <div className="filter-header">
        <h3>Filters</h3>
        {onToggle && (
          <button onClick={onToggle} aria-label="Collapse filters">
            &times;
          </button>
        )}
      </div>

      <fieldset className="filter-section">
        <legend>Kind</legend>
        {ALL_KINDS.map((kind) => (
          <label key={kind} className="filter-checkbox">
            <input
              type="checkbox"
              checked={values.kinds.includes(kind)}
              onChange={() => toggleKind(kind)}
            />
            <span>{KIND_LABELS[kind]}</span>
          </label>
        ))}
      </fieldset>

      <fieldset className="filter-section">
        <legend>Lifecycle</legend>
        <select
          value={values.lifecycle}
          onChange={(e) =>
            setField('lifecycle', e.target.value as MemoryStatus | '')
          }
        >
          <option value="">All</option>
          {ALL_STATUSES.map((s) => (
            <option key={s} value={s}>
              {s.charAt(0).toUpperCase() + s.slice(1)}
            </option>
          ))}
        </select>
      </fieldset>

      <fieldset className="filter-section">
        <legend>Sensitivity</legend>
        <div className="filter-radio-group">
          <label className="filter-radio">
            <input
              type="radio"
              name="sensitivity"
              value=""
              checked={values.sensitivity === ''}
              onChange={() => setField('sensitivity', '')}
            />
            <span>All</span>
          </label>
          {ALL_SENSITIVITIES.map((s) => (
            <label key={s} className="filter-radio">
              <input
                type="radio"
                name="sensitivity"
                value={s}
                checked={values.sensitivity === s}
                onChange={() => setField('sensitivity', s)}
              />
              <span>{s.charAt(0).toUpperCase() + s.slice(1)}</span>
            </label>
          ))}
        </div>
      </fieldset>

      <fieldset className="filter-section">
        <legend>Date Range</legend>
        <div className="filter-date-group">
          <label>
            From:
            <input
              type="date"
              value={values.createdAfter}
              onChange={(e) => setField('createdAfter', e.target.value)}
            />
          </label>
          <label>
            To:
            <input
              type="date"
              value={values.createdBefore}
              onChange={(e) => setField('createdBefore', e.target.value)}
            />
          </label>
        </div>
      </fieldset>
    </aside>
  );
}

export function defaultFilterValues(): FilterValues {
  return {
    kinds: [],
    lifecycle: '',
    sensitivity: '',
    createdAfter: '',
    createdBefore: '',
  };
}
