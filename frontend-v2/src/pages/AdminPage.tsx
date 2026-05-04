import { useState, useEffect } from 'react';
import type { DomainProfile, TenantConfig } from '../types';
import {
  getTenantConfig,
  getProfiles,
  reindex,
} from '../services/api';

export default function AdminPage() {
  const [tenant, setTenant] = useState<TenantConfig | null>(null);
  const [profiles, setProfiles] = useState<DomainProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reindexStatus, setReindexStatus] = useState<string | null>(null);
  const [reindexing, setReindexing] = useState(false);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [t, p] = await Promise.all([
          getTenantConfig(),
          getProfiles(),
        ]);
        if (!cancelled) {
          setTenant(t);
          setProfiles(p);
        }
      } catch (err: unknown) {
        if (!cancelled) {
          const apiErr = err as { message?: string };
          setError(apiErr?.message || 'Failed to load admin data.');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, []);

  async function handleReindex() {
    if (!window.confirm('Are you sure you want to reindex all memories? This may take some time.'))
      return;
    setReindexing(true);
    setReindexStatus(null);
    try {
      const result = await reindex();
      setReindexStatus(result.message || 'Reindex completed successfully.');
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setReindexStatus(`Reindex failed: ${apiErr?.message || 'Unknown error'}`);
    } finally {
      setReindexing(false);
    }
  }

  if (loading) {
    return <div className="loading-spinner">Loading admin panel...</div>;
  }

  if (error) {
    return (
      <div className="error-message" role="alert">
        {error}
      </div>
    );
  }

  return (
    <div className="admin-page">
      <h2>Administration</h2>

      {reindexStatus && (
        <div
          className={
            reindexStatus.includes('failed') ||
            reindexStatus.includes('Failed')
              ? 'error-message'
              : 'success-message'
          }
          role="status"
        >
          {reindexStatus}
        </div>
      )}

      <section className="admin-section">
        <h3>Tenant Configuration</h3>
        {tenant && (
          <table className="detail-table">
            <tbody>
              <tr>
                <th>Tenant ID</th>
                <td>{tenant.tenantId}</td>
              </tr>
              <tr>
                <th>Name</th>
                <td>{tenant.name}</td>
              </tr>
              <tr>
                <th>Rate Limit (per minute)</th>
                <td>{tenant.rateLimitPerMinute}</td>
              </tr>
              <tr>
                <th>Active Profile</th>
                <td>{tenant.activeProfileId || 'None (Core Generic)'}</td>
              </tr>
            </tbody>
          </table>
        )}
      </section>

      <section className="admin-section">
        <h3>Domain Profiles ({profiles.length})</h3>
        {profiles.length > 0 ? (
          <table className="detail-table admin-profiles-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Kinds</th>
                <th>Default Sensitivity</th>
                <th>Default Confidence</th>
                <th>Default Importance</th>
              </tr>
            </thead>
            <tbody>
              {profiles.map((p) => (
                <tr key={p.id}>
                  <td>{p.name}</td>
                  <td>
                    <div className="chip-group">
                      {p.kinds.map((k) => (
                        <span key={k} className="kind-chip-sm">
                          {k}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td>{p.defaultSensitivity}</td>
                  <td>{(p.defaultConfidence * 100).toFixed(0)}%</td>
                  <td>{(p.defaultImportance * 100).toFixed(0)}%</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p>No domain profiles configured.</p>
        )}
      </section>

      <section className="admin-section">
        <h3>Maintenance</h3>
        <div className="admin-actions">
          <button
            className="btn btn-warning"
            onClick={handleReindex}
            disabled={reindexing}
          >
            {reindexing ? 'Reindexing...' : 'Re-index All Memories'}
          </button>
          <span className="field-hint">
            Rebuilds the semantic search index for all memories. Use after
            importing data or fixing index inconsistencies.
          </span>
        </div>
      </section>
    </div>
  );
}
