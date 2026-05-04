import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import ConfidenceBar from '../components/ConfidenceBar';

describe('ConfidenceBar', () => {
  it('renders with correct ARIA progressbar attributes', () => {
    render(<ConfidenceBar value={0.75} />);
    const bar = screen.getByRole('progressbar');
    expect(bar).toBeInTheDocument();
    expect(bar).toHaveAttribute('aria-valuenow', '0.75');
    expect(bar).toHaveAttribute('aria-valuemin', '0');
    expect(bar).toHaveAttribute('aria-valuemax', '1');
  });

  it('renders with a default label showing percentage', () => {
    render(<ConfidenceBar value={0.5} />);
    expect(screen.getByText('Score: 50%')).toBeInTheDocument();
  });

  it('renders with a custom label', () => {
    render(<ConfidenceBar value={0.8} label="Confidence: 80%" />);
    expect(screen.getByText('Confidence: 80%')).toBeInTheDocument();
  });

  it('renders fill bar with correct width', () => {
    const { container } = render(<ConfidenceBar value={0.25} />);
    const fill = container.querySelector('.confidence-bar-fill') as HTMLElement;
    expect(fill).toBeInTheDocument();
    expect(fill.style.width).toBe('25%');
  });

  it('clamps value to 0–100% range', () => {
    const { container: c1 } = render(<ConfidenceBar value={1.5} />);
    const fill1 = c1.querySelector('.confidence-bar-fill') as HTMLElement;
    expect(fill1.style.width).toBe('100%');

    const { container: c2 } = render(<ConfidenceBar value={-0.5} />);
    const fill2 = c2.querySelector('.confidence-bar-fill') as HTMLElement;
    expect(fill2.style.width).toBe('0%');
  });
});
