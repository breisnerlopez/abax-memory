import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import KindBadge from '../components/KindBadge';

describe('KindBadge', () => {
  it('renders the correct label for a given kind', () => {
    render(<KindBadge kind="fact" />);
    expect(screen.getByText('Fact')).toBeInTheDocument();
  });

  it('renders all 8 kind labels correctly', () => {
    const kinds = [
      'fact', 'preference', 'event', 'decision',
      'task', 'procedure', 'note', 'entity',
    ] as const;
    const expectedLabels = [
      'Fact', 'Preference', 'Event', 'Decision',
      'Task', 'Procedure', 'Note', 'Entity',
    ];

    kinds.forEach((kind, idx) => {
      const { unmount } = render(<KindBadge kind={kind} />);
      expect(screen.getByText(expectedLabels[idx])).toBeInTheDocument();
      unmount();
    });
  });

  it('applies the correct background color for each kind', () => {
    const { container } = render(<KindBadge kind="fact" />);
    const badge = container.querySelector('.kind-badge') as HTMLElement;
    expect(badge).toBeInTheDocument();
    expect(badge.style.backgroundColor).toBe('rgb(59, 130, 246)');
  });

  it('renders with an aria-label for accessibility', () => {
    render(<KindBadge kind="decision" />);
    expect(screen.getByLabelText('Kind: Decision')).toBeInTheDocument();
  });
});
