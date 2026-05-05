import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import FilterPanel, { defaultFilterValues } from '../components/FilterPanel';

describe('FilterPanel', () => {
  it('renders filter sections when not collapsed', () => {
    const values = defaultFilterValues();
    render(
      <FilterPanel values={values} onChange={() => {}} />
    );
    expect(screen.getByText('Filters')).toBeInTheDocument();
    expect(screen.getByText('Kind')).toBeInTheDocument();
    expect(screen.getByText('Lifecycle')).toBeInTheDocument();
    expect(screen.getByText('Sensitivity')).toBeInTheDocument();
    expect(screen.getByText('Date Range')).toBeInTheDocument();
  });

  it('renders a toggle button when collapsed', () => {
    const values = defaultFilterValues();
    render(
      <FilterPanel values={values} onChange={() => {}} collapsed />
    );
    expect(screen.getByText(/Filters/)).toBeInTheDocument();
    expect(screen.queryByText('Kind')).not.toBeInTheDocument();
  });

  it('calls onChange when a kind checkbox is toggled', () => {
    const handleChange = vi.fn();
    const values = defaultFilterValues();
    render(
      <FilterPanel values={values} onChange={handleChange} />
    );
    // Click "Fact" checkbox
    const factCheckbox = screen.getByLabelText('Fact');
    fireEvent.click(factCheckbox);
    expect(handleChange).toHaveBeenCalledWith(
      expect.objectContaining({ kinds: ['fact'] })
    );
  });

  it('calls onChange when lifecycle dropdown changes', () => {
    const handleChange = vi.fn();
    const values = defaultFilterValues();
    render(
      <FilterPanel values={values} onChange={handleChange} />
    );
    const select = screen.getByRole('combobox');
    fireEvent.change(select, { target: { value: 'active' } });
    expect(handleChange).toHaveBeenCalledWith(
      expect.objectContaining({ lifecycle: 'active' })
    );
  });

  it('calls onToggle when collapse button is clicked', () => {
    const handleToggle = vi.fn();
    const values = defaultFilterValues();
    render(
      <FilterPanel
        values={values}
        onChange={() => {}}
        onToggle={handleToggle}
      />
    );
    const closeBtn = screen.getByLabelText('Collapse filters');
    fireEvent.click(closeBtn);
    expect(handleToggle).toHaveBeenCalled();
  });

  it('renders all 8 kind checkboxes', () => {
    const values = defaultFilterValues();
    render(<FilterPanel values={values} onChange={() => {}} />);
    const labels = [
      'Fact', 'Preference', 'Event', 'Decision',
      'Task', 'Procedure', 'Note', 'Entity',
    ];
    labels.forEach((label) => {
      expect(screen.getByText(label)).toBeInTheDocument();
    });
  });

  it('renders all sensitivity radio options', () => {
    const values = defaultFilterValues();
    render(<FilterPanel values={values} onChange={() => {}} />);
    // 'All' appears in both lifecycle dropdown and sensitivity radio
    const allElements = screen.getAllByText('All');
    expect(allElements.length).toBeGreaterThanOrEqual(2);
    expect(screen.getByText('Public')).toBeInTheDocument();
    expect(screen.getByText('Internal')).toBeInTheDocument();
    expect(screen.getByText('Confidential')).toBeInTheDocument();
    expect(screen.getByText('Secret')).toBeInTheDocument();
  });
});
