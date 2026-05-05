import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import LifecycleBadge from '../components/LifecycleBadge';

describe('LifecycleBadge', () => {
  it('renders the correct label for active status', () => {
    render(<LifecycleBadge status="active" />);
    expect(screen.getByText('Active')).toBeInTheDocument();
  });

  it('renders the correct label for pending status', () => {
    render(<LifecycleBadge status="pending" />);
    expect(screen.getByText('Pending')).toBeInTheDocument();
  });

  it('renders the correct label for rejected status', () => {
    render(<LifecycleBadge status="rejected" />);
    expect(screen.getByText('Rejected')).toBeInTheDocument();
  });

  it('renders all lifecycle statuses with correct colors', () => {
    const statuses = ['draft', 'pending', 'active', 'archived', 'rejected', 'deleted'] as const;

    statuses.forEach((status) => {
      const { container, unmount } = render(<LifecycleBadge status={status} />);
      const badge = container.querySelector('.lifecycle-badge') as HTMLElement;
      expect(badge).toBeInTheDocument();
      unmount();
    });
  });

  it('includes aria-label for accessibility', () => {
    render(<LifecycleBadge status="archived" />);
    expect(screen.getByLabelText('Status: Archived')).toBeInTheDocument();
  });
});
