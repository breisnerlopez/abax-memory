import { useState, useEffect, useCallback } from 'react';
import type { MemoryFragment } from '../types';
import { searchMemories, reviewMemory } from '../services/api';
import MemoryCard from '../components/MemoryCard';
import { useNavigate } from 'react-router-dom';

interface ReviewItem {
  memory: MemoryFragment;
  comment: string;
  submitting: boolean;
}

export default function ReviewPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<ReviewItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const loadPending = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await searchMemories({
        query: '',
        filters: { statuses: ['pending'] },
        topK: 50,
        pageSize: 50,
      });
      const reviewItems: ReviewItem[] = data.results.map((r) => ({
        memory: r.memory,
        comment: '',
        submitting: false,
      }));
      setItems(reviewItems);
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr?.message || 'Failed to load pending reviews.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadPending();
  }, [loadPending]);

  function setComment(memoryId: string, comment: string) {
    setItems((prev) =>
      prev.map((item) =>
        item.memory.id === memoryId ? { ...item, comment } : item
      )
    );
  }

  function setSubmitting(memoryId: string, submitting: boolean) {
    setItems((prev) =>
      prev.map((item) =>
        item.memory.id === memoryId ? { ...item, submitting } : item
      )
    );
  }

  async function handleReview(
    memoryId: string,
    action: 'approve' | 'reject' | 'request_changes'
  ) {
    const item = items.find((i) => i.memory.id === memoryId);
    if (!item) return;

    setSubmitting(memoryId, true);
    setActionMessage(null);
    setError(null);

    try {
      await reviewMemory(memoryId, {
        action,
        comment: item.comment || undefined,
      });
      const actionLabel =
        action === 'approve'
          ? 'approved'
          : action === 'reject'
            ? 'rejected'
            : 'returned to draft';
      setActionMessage(
        `Memory ${memoryId} ${actionLabel}.`
      );
      // Remove from list
      setItems((prev) => prev.filter((i) => i.memory.id !== memoryId));
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr?.message || `Failed to ${action} memory.`);
    } finally {
      setSubmitting(memoryId, false);
    }
  }

  function handleMemoryClick(id: string) {
    navigate(`/detail/${id}`);
  }

  return (
    <div className="review-page">
      <h2>Review Queue</h2>

      {actionMessage && (
        <div className="success-message" role="status">
          {actionMessage}
        </div>
      )}

      {error && (
        <div className="error-message" role="alert">
          {error}
        </div>
      )}

      <div className="review-controls">
        <button
          className="btn btn-secondary"
          onClick={loadPending}
          disabled={loading}
        >
          {loading ? 'Loading...' : 'Refresh'}
        </button>
        <span className="review-count">
          {items.length} pending review{items.length !== 1 ? 's' : ''}
        </span>
      </div>

      {loading && <div className="loading-spinner">Loading pending reviews...</div>}

      {!loading && items.length === 0 && (
        <p className="no-results">No memories pending review.</p>
      )}

      <div className="review-list">
        {items.map((item) => (
          <div key={item.memory.id} className="review-item">
            <MemoryCard
              memory={item.memory}
              onClick={handleMemoryClick}
            />
            <div className="review-actions">
              <textarea
                className="review-comment"
                placeholder="Optional review comment..."
                value={item.comment}
                onChange={(e) =>
                  setComment(item.memory.id, e.target.value)
                }
                rows={2}
                aria-label={`Review comment for ${item.memory.id}`}
              />
              <div className="review-buttons">
                <button
                  className="btn btn-success"
                  onClick={() => handleReview(item.memory.id, 'approve')}
                  disabled={item.submitting}
                >
                  {item.submitting ? '...' : 'Approve'}
                </button>
                <button
                  className="btn btn-warning"
                  onClick={() =>
                    handleReview(item.memory.id, 'request_changes')
                  }
                  disabled={item.submitting}
                >
                  {item.submitting ? '...' : 'Request Changes'}
                </button>
                <button
                  className="btn btn-danger"
                  onClick={() => handleReview(item.memory.id, 'reject')}
                  disabled={item.submitting}
                >
                  {item.submitting ? '...' : 'Reject'}
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
