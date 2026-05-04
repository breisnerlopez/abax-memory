import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import Pagination from '../components/Pagination';

describe('Pagination', () => {
  it('renders nothing when totalPages is 1', () => {
    const { container } = render(
      <Pagination page={1} totalPages={1} onPageChange={() => {}} />
    );
    expect(container.querySelector('.pagination')).not.toBeInTheDocument();
  });

  it('renders page buttons for multiple pages', () => {
    render(
      <Pagination page={1} totalPages={5} onPageChange={() => {}} />
    );
    expect(screen.getByText('1')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('disables Previous button on first page', () => {
    render(
      <Pagination page={1} totalPages={5} onPageChange={() => {}} />
    );
    const prevBtn = screen.getByLabelText('Previous page');
    expect(prevBtn).toBeDisabled();
  });

  it('disables Next button on last page', () => {
    render(
      <Pagination page={5} totalPages={5} onPageChange={() => {}} />
    );
    const nextBtn = screen.getByLabelText('Next page');
    expect(nextBtn).toBeDisabled();
  });

  it('calls onPageChange when a page button is clicked', () => {
    const handleChange = vi.fn();
    render(
      <Pagination page={1} totalPages={5} onPageChange={handleChange} />
    );
    fireEvent.click(screen.getByText('3'));
    expect(handleChange).toHaveBeenCalledWith(3);
  });

  it('calls onPageChange when Next button is clicked', () => {
    const handleChange = vi.fn();
    render(
      <Pagination page={2} totalPages={5} onPageChange={handleChange} />
    );
    fireEvent.click(screen.getByLabelText('Next page'));
    expect(handleChange).toHaveBeenCalledWith(3);
  });

  it('marks the current page as active', () => {
    render(
      <Pagination page={3} totalPages={5} onPageChange={() => {}} />
    );
    const activeBtn = screen.getByText('3');
    expect(activeBtn).toHaveAttribute('aria-current', 'page');
    expect(activeBtn.className).toContain('pagination-active');
  });
});
