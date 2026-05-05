import { useState, useCallback } from 'react';
import type { SearchRequest } from '../types';
import { searchMemories } from '../services/api';
import MemoryCard from '../components/MemoryCard';
import FilterPanel, {
  defaultFilterValues,
  type FilterValues,
} from '../components/FilterPanel';
import Pagination from '../components/Pagination';
import { useNavigate } from 'react-router-dom';

export default function SearchPage() {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [filters, setFilters] = useState<FilterValues>(defaultFilterValues());
  const [filtersVisible, setFiltersVisible] = useState(true);
  const [results, setResults] = useState<Awaited<
    ReturnType<typeof searchMemories>
  > | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);

  const handleSearch = useCallback(
    async (searchPage: number = 1) => {
      setLoading(true);
      setError(null);

      try {
        const searchReq: SearchRequest = {
          query,
          topK: 20,
          page: searchPage,
          pageSize: 10,
        };

        if (
          filters.kinds.length > 0 ||
          filters.lifecycle ||
          filters.sensitivity ||
          filters.createdAfter ||
          filters.createdBefore
        ) {
          searchReq.filters = {};
          if (filters.kinds.length > 0)
            searchReq.filters.kinds = filters.kinds;
          if (filters.lifecycle)
            searchReq.filters.statuses = [filters.lifecycle];
          if (filters.sensitivity)
            searchReq.filters.sensitivities = [filters.sensitivity];
          if (filters.createdAfter)
            searchReq.filters.createdAfter = filters.createdAfter;
          if (filters.createdBefore)
            searchReq.filters.createdBefore = filters.createdBefore;
        }

        const data = await searchMemories(searchReq);
        setResults(data);
        setPage(searchPage);
      } catch (err: unknown) {
        const apiErr = err as { message?: string };
        setError(apiErr?.message || 'Search failed. Please try again.');
      } finally {
        setLoading(false);
      }
    },
    [query, filters]
  );

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    handleSearch(1);
  };

  const handleMemoryClick = (id: string) => {
    navigate(`/detail/${id}`);
  };

  return (
    <div className="search-page">
      <form className="search-form" onSubmit={handleSubmit}>
        <div className="search-bar">
          <input
            type="text"
            className="search-input"
            placeholder="Search memories semantically..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            aria-label="Search query"
          />
          <button type="submit" className="search-btn" disabled={loading}>
            {loading ? 'Searching...' : 'Search'}
          </button>
          <button
            type="button"
            className="filter-toggle-btn"
            onClick={() => setFiltersVisible(!filtersVisible)}
          >
            {filtersVisible ? 'Hide Filters' : 'Show Filters'}
          </button>
        </div>
      </form>

      <div className="search-content">
        {filtersVisible && (
          <FilterPanel
            values={filters}
            onChange={setFilters}
            onToggle={() => setFiltersVisible(false)}
          />
        )}

        {!filtersVisible && (
          <FilterPanel
            values={filters}
            onChange={setFilters}
            collapsed
            onToggle={() => setFiltersVisible(true)}
          />
        )}

        <section className="search-results" aria-live="polite">
          {error && (
            <div className="error-message" role="alert">
              {error}
            </div>
          )}

          {loading && (
            <div className="loading-spinner" aria-busy="true">
              Searching...
            </div>
          )}

          {results && !loading && (
            <>
              <div className="results-header">
                <span>
                  Found {results.total} result{results.total !== 1 ? 's' : ''}
                </span>
                {results.facets && (
                  <div className="results-facets">
                    {Object.entries(results.facets.kinds || {}).map(
                      ([kind, count]) => (
                        <span key={kind} className="facet-chip">
                          {kind}: {count}
                        </span>
                      )
                    )}
                  </div>
                )}
              </div>

              <div className="results-list">
                {results.results.map((r) => (
                  <MemoryCard
                    key={r.memory.id}
                    memory={r.memory}
                    score={r.score}
                    onClick={handleMemoryClick}
                  />
                ))}
              </div>

              {results.results.length === 0 && (
                <p className="no-results">No memories found.</p>
              )}

              <Pagination
                page={page}
                totalPages={results.totalPages}
                onPageChange={(p) => handleSearch(p)}
              />
            </>
          )}

          {!results && !loading && !error && (
            <div className="search-empty-state">
              <p>Enter a query to search memories across all domains.</p>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
