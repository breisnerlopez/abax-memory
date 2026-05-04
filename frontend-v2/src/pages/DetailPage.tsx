import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import type { MemoryFragment, GraphResponse, RelationItem } from '../types';
import {
  getMemory,
  getRelations,
  expandGraph,
  deleteMemory,
} from '../services/api';
import KindBadge from '../components/KindBadge';
import LifecycleBadge from '../components/LifecycleBadge';
import ConfidenceBar from '../components/ConfidenceBar';
import { SENSITIVITY_COLORS } from '../types';

export default function DetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [memory, setMemory] = useState<MemoryFragment | null>(null);
  const [relations, setRelations] = useState<RelationItem[]>([]);
  const [graph, setGraph] = useState<GraphResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showGraph, setShowGraph] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    if (!id) return;
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [mem, rels] = await Promise.all([
          getMemory(id!),
          getRelations(id!),
        ]);
        if (!cancelled) {
          setMemory(mem);
          setRelations(rels);
        }
      } catch (err: unknown) {
        if (!cancelled) {
          const apiErr = err as { message?: string };
          setError(apiErr?.message || 'Failed to load memory.');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [id]);

  async function handleExpandGraph() {
    if (!id) return;
    if (showGraph) {
      setShowGraph(false);
      return;
    }
    try {
      const g = await expandGraph(id, 2);
      setGraph(g);
      setShowGraph(true);
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr?.message || 'Failed to expand graph.');
    }
  }

  async function handleDelete() {
    if (!id || !window.confirm('Are you sure you want to delete this memory?'))
      return;
    setDeleting(true);
    try {
      await deleteMemory(id);
      navigate('/', { replace: true });
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setError(apiErr?.message || 'Delete failed.');
    } finally {
      setDeleting(false);
    }
  }

  if (loading) {
    return <div className="loading-spinner">Loading memory...</div>;
  }

  if (error) {
    return (
      <div className="error-message" role="alert">
        {error}
        <button onClick={() => navigate(-1)}>Go back</button>
      </div>
    );
  }

  if (!memory) {
    return <div className="error-message">Memory not found.</div>;
  }

  const sensitivityColor =
    SENSITIVITY_COLORS[memory.lifecycle.sensitivity] || '#6b7280';

  return (
    <div className="detail-page">
      <nav className="breadcrumb">
        <button onClick={() => navigate(-1)}>&larr; Back</button>
      </nav>

      <article className="detail-content">
        <header className="detail-header">
          <div className="detail-badges">
            <KindBadge kind={memory.kind} />
            <LifecycleBadge status={memory.lifecycle.status} />
            <span
              className="sensitivity-badge"
              style={{ backgroundColor: sensitivityColor }}
            >
              {memory.lifecycle.sensitivity}
            </span>
          </div>
          <h2>{memory.summary || memory.id}</h2>
          <div className="detail-meta">
            <span>ID: {memory.id}</span>
            <span>Created: {new Date(memory.createdAt).toLocaleString()}</span>
            <span>Updated: {new Date(memory.updatedAt).toLocaleString()}</span>
          </div>
        </header>

        <section className="detail-section">
          <h3>Content</h3>
          <div className="detail-content-body">{memory.content}</div>
        </section>

        <section className="detail-section">
          <h3>Scores</h3>
          <div className="detail-scores">
            <div>
              <label>Confidence</label>
              <ConfidenceBar value={memory.lifecycle.confidence} />
            </div>
            <div>
              <label>Importance</label>
              <ConfidenceBar
                value={memory.lifecycle.importance}
                label={`Importance: ${(memory.lifecycle.importance * 100).toFixed(0)}%`}
              />
            </div>
          </div>
        </section>

        <section className="detail-section">
          <h3>Metadata</h3>
          <table className="detail-table">
            <tbody>
              <tr>
                <th>Source Type</th>
                <td>{memory.source.type}</td>
              </tr>
              <tr>
                <th>Source Ref</th>
                <td>{memory.source.ref || '—'}</td>
              </tr>
              <tr>
                <th>Tenant</th>
                <td>{memory.scope.tenantId}</td>
              </tr>
              {memory.scope.userId && (
                <tr>
                  <th>User</th>
                  <td>{memory.scope.userId}</td>
                </tr>
              )}
              {memory.scope.sessionId && (
                <tr>
                  <th>Session</th>
                  <td>{memory.scope.sessionId}</td>
                </tr>
              )}
              {Object.entries(memory.metadata || {}).map(([key, val]) => (
                <tr key={key}>
                  <th>{key}</th>
                  <td>{val}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        {memory.topics.length > 0 && (
          <section className="detail-section">
            <h3>Topics</h3>
            <div className="chip-group">
              {memory.topics.map((t) => (
                <span key={t} className="topic-chip">
                  {t}
                </span>
              ))}
            </div>
          </section>
        )}

        {memory.entities.length > 0 && (
          <section className="detail-section">
            <h3>Entities</h3>
            <div className="chip-group">
              {memory.entities.map((e) => (
                <span key={e} className="entity-chip">
                  {e}
                </span>
              ))}
            </div>
          </section>
        )}

        <section className="detail-section">
          <h3>Relations ({relations.length})</h3>
          {relations.length > 0 ? (
            <ul className="relations-list">
              {relations.map((r) => (
                <li key={r.id} className="relation-item">
                  <span className="relation-type">{r.type}</span>
                  <button
                    className="relation-link"
                    onClick={() => navigate(`/detail/${r.targetId}`)}
                  >
                    {r.targetId}
                  </button>
                </li>
              ))}
            </ul>
          ) : (
            <p>No relations.</p>
          )}
        </section>

        {showGraph && graph && (
          <section className="detail-section">
            <h3>Graph (depth: {graph.depth})</h3>
            <div className="graph-container">
              <ul className="graph-node-list">
                <li className="graph-node graph-node-root">
                  {graph.root.summary || graph.root.memoryId}
                </li>
                {graph.nodes.map((n) => (
                  <li
                    key={n.memoryId}
                    className="graph-node"
                    onClick={() => navigate(`/detail/${n.memoryId}`)}
                  >
                    {n.summary || n.memoryId}
                  </li>
                ))}
              </ul>
              {graph.edges.length > 0 && (
                <div className="graph-edges">
                  {graph.edges.map((e, i) => (
                    <span key={i} className="graph-edge">
                      {e.sourceId} → {e.targetId} ({e.type})
                    </span>
                  ))}
                </div>
              )}
            </div>
          </section>
        )}

        <section className="detail-actions">
          <button
            className="btn btn-primary"
            onClick={() => navigate(`/edit/${memory.id}`)}
          >
            Edit
          </button>
          <button
            className="btn btn-secondary"
            onClick={() => navigate(`/review`)}
          >
            Request Review
          </button>
          <button className="btn btn-secondary" onClick={handleExpandGraph}>
            {showGraph ? 'Hide Graph' : 'Expand Graph'}
          </button>
          <button
            className="btn btn-danger"
            onClick={handleDelete}
            disabled={deleting}
          >
            {deleting ? 'Deleting...' : 'Delete'}
          </button>
        </section>
      </article>
    </div>
  );
}
