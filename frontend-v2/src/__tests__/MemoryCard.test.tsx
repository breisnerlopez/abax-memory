import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import MemoryCard from '../components/MemoryCard';
import type { MemoryFragment } from '../types';

const mockMemory: MemoryFragment = {
  id: 'MEM-test1234',
  kind: 'fact',
  content: 'This is a test memory fragment content for verification.',
  summary: 'Test Memory Summary',
  topics: ['testing', 'frontend'],
  entities: ['Abax'],
  relations: [],
  metadata: {},
  source: { type: 'manual', ref: '' },
  scope: { tenantId: 'tenant-001' },
  lifecycle: {
    status: 'active',
    confidence: 0.85,
    importance: 0.7,
    sensitivity: 'internal',
    reviewedBy: null,
    reviewedAt: null,
  },
  createdAt: '2026-05-01T10:00:00Z',
  updatedAt: '2026-05-02T12:00:00Z',
};

describe('MemoryCard', () => {
  it('renders the memory summary as snippet', () => {
    render(<MemoryCard memory={mockMemory} onClick={() => {}} />);
    expect(screen.getByText('Test Memory Summary')).toBeInTheDocument();
  });

  it('renders the kind badge', () => {
    render(<MemoryCard memory={mockMemory} onClick={() => {}} />);
    expect(screen.getByText('Fact')).toBeInTheDocument();
  });

  it('renders the lifecycle badge', () => {
    render(<MemoryCard memory={mockMemory} onClick={() => {}} />);
    expect(screen.getByText('Active')).toBeInTheDocument();
  });

  it('renders topic chips', () => {
    render(<MemoryCard memory={mockMemory} onClick={() => {}} />);
    expect(screen.getByText('testing')).toBeInTheDocument();
    expect(screen.getByText('frontend')).toBeInTheDocument();
  });

  it('renders the score bar when score is provided', () => {
    render(<MemoryCard memory={mockMemory} score={0.92} onClick={() => {}} />);
    expect(screen.getByText('Score: 92%')).toBeInTheDocument();
  });

  it('does not render the score bar when score is undefined', () => {
    render(<MemoryCard memory={mockMemory} onClick={() => {}} />);
    expect(screen.queryByText(/Score:/)).not.toBeInTheDocument();
  });

  it('calls onClick when clicked', () => {
    const handleClick = vi.fn();
    render(<MemoryCard memory={mockMemory} onClick={handleClick} />);
    fireEvent.click(screen.getByRole('button'));
    expect(handleClick).toHaveBeenCalledWith('MEM-test1234');
  });

  it('uses summary fallback when summary is empty', () => {
    const memWithoutSummary = {
      ...mockMemory,
      summary: '',
      content: 'Only content text here.',
    };
    render(<MemoryCard memory={memWithoutSummary} onClick={() => {}} />);
    expect(screen.getByText(/Only content text here/)).toBeInTheDocument();
  });

  it('is keyboard-accessible with Enter key', () => {
    const handleClick = vi.fn();
    render(<MemoryCard memory={mockMemory} onClick={handleClick} />);
    fireEvent.keyDown(screen.getByRole('button'), { key: 'Enter' });
    expect(handleClick).toHaveBeenCalledWith('MEM-test1234');
  });
});
