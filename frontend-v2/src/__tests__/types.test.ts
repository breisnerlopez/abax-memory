import { describe, it, expect } from 'vitest';
import {
  KIND_COLORS,
  KIND_LABELS,
  LIFECYCLE_COLORS,
  SENSITIVITY_COLORS,
} from '../types';

describe('Type constants', () => {
  it('KIND_COLORS has entries for all 8 kinds', () => {
    const kinds = ['fact', 'preference', 'event', 'decision', 'task', 'procedure', 'note', 'entity'];
    kinds.forEach((k) => {
      expect(KIND_COLORS).toHaveProperty(k);
      expect(typeof KIND_COLORS[k as keyof typeof KIND_COLORS]).toBe('string');
    });
  });

  it('KIND_LABELS has entries for all 8 kinds', () => {
    const kinds = ['fact', 'preference', 'event', 'decision', 'task', 'procedure', 'note', 'entity'];
    kinds.forEach((k) => {
      expect(KIND_LABELS).toHaveProperty(k);
      expect(typeof KIND_LABELS[k as keyof typeof KIND_LABELS]).toBe('string');
    });
  });

  it('LIFECYCLE_COLORS has entries for all 6 statuses', () => {
    const statuses = ['draft', 'pending', 'active', 'archived', 'rejected', 'deleted'];
    statuses.forEach((s) => {
      expect(LIFECYCLE_COLORS).toHaveProperty(s);
    });
  });

  it('SENSITIVITY_COLORS has entries for all 4 levels', () => {
    const levels = ['public', 'internal', 'confidential', 'secret'];
    levels.forEach((l) => {
      expect(SENSITIVITY_COLORS).toHaveProperty(l);
    });
  });

  it('KIND_LABELS are in PascalCase English', () => {
    expect(KIND_LABELS.fact).toBe('Fact');
    expect(KIND_LABELS.preference).toBe('Preference');
    expect(KIND_LABELS.event).toBe('Event');
    expect(KIND_LABELS.decision).toBe('Decision');
    expect(KIND_LABELS.task).toBe('Task');
    expect(KIND_LABELS.procedure).toBe('Procedure');
    expect(KIND_LABELS.note).toBe('Note');
    expect(KIND_LABELS.entity).toBe('Entity');
  });
});
