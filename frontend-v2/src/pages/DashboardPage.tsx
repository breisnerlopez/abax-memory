import { useState, useEffect } from 'react';
import type { DashboardStats } from '../types';
import { getDashboardStats } from '../services/api';
import { KIND_COLORS, KIND_LABELS, LIFECYCLE_COLORS } from '../types';

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const data = await getDashboardStats();
        if (!cancelled) setStats(data);
      } catch (err: unknown) {
        if (!cancelled) {
          const apiErr = err as { message?: string };
          setError(apiErr?.message || 'Failed to load dashboard stats.');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, []);

  if (loading) {
    return <div className="loading-spinner">Loading dashboard...</div>;
  }

  if (error) {
    return (
      <div className="error-message" role="alert">
        {error}
      </div>
    );
  }

  if (!stats) {
    return <p>No data available.</p>;
  }

  const maxKindCount = Math.max(1, ...Object.values(stats.byKind));
  const maxLifecycleCount = Math.max(
    1,
    ...Object.values(stats.byLifecycle)
  );

  return (
    <div className="dashboard-page">
      <h2>Dashboard</h2>

      {/* KPI Cards */}
      <section className="dashboard-kpis">
        <div className="kpi-card">
          <span className="kpi-value">{stats.totalFragments}</span>
          <span className="kpi-label">Total Fragments</span>
        </div>
        <div className="kpi-card">
          <span className="kpi-value">{stats.recentActivity}</span>
          <span className="kpi-label">Recent Activity (7d)</span>
        </div>
        <div className="kpi-card">
          <span className="kpi-value">
            {stats.reviewRate.approved}/{stats.reviewRate.rejected}
          </span>
          <span className="kpi-label">Approved / Rejected</span>
        </div>
      </section>

      {/* By Kind — Horizontal Bar Chart (pure CSS) */}
      <section className="dashboard-section">
        <h3>By Kind</h3>
        <div className="bar-chart" aria-label="Fragments by kind">
          {Object.entries(stats.byKind)
            .sort(([, a], [, b]) => b - a)
            .map(([kind, count]) => {
              const widthPct = (count / maxKindCount) * 100;
              const color =
                KIND_COLORS[kind as keyof typeof KIND_COLORS] || '#a3a3a3';
              const label =
                KIND_LABELS[kind as keyof typeof KIND_LABELS] || kind;
              return (
                <div key={kind} className="bar-row">
                  <span className="bar-label">{label}</span>
                  <div className="bar-track">
                    <div
                      className="bar-fill"
                      style={{ width: `${widthPct}%`, backgroundColor: color }}
                      role="img"
                      aria-label={`${label}: ${count}`}
                    />
                  </div>
                  <span className="bar-value">{count}</span>
                </div>
              );
            })}
        </div>
      </section>

      {/* By Lifecycle — Horizontal Bar Chart */}
      <section className="dashboard-section">
        <h3>By Lifecycle</h3>
        <div className="bar-chart" aria-label="Fragments by lifecycle status">
          {Object.entries(stats.byLifecycle)
            .sort(([, a], [, b]) => b - a)
            .map(([status, count]) => {
              const widthPct = (count / maxLifecycleCount) * 100;
              const color =
                LIFECYCLE_COLORS[
                  status as keyof typeof LIFECYCLE_COLORS
                ] || '#6b7280';
              const label =
                status.charAt(0).toUpperCase() + status.slice(1);
              return (
                <div key={status} className="bar-row">
                  <span className="bar-label">{label}</span>
                  <div className="bar-track">
                    <div
                      className="bar-fill"
                      style={{ width: `${widthPct}%`, backgroundColor: color }}
                      role="img"
                      aria-label={`${label}: ${count}`}
                    />
                  </div>
                  <span className="bar-value">{count}</span>
                </div>
              );
            })}
        </div>
      </section>

      {/* Review Rate — Simple visual */}
      <section className="dashboard-section">
        <h3>Review Rate</h3>
        <div className="review-rate">
          <div className="review-rate-bar">
            {stats.reviewRate.approved + stats.reviewRate.rejected > 0 && (
              <>
                <div
                  className="review-rate-approved"
                  style={{
                    width: `${
                      (stats.reviewRate.approved /
                        (stats.reviewRate.approved +
                          stats.reviewRate.rejected)) *
                      100
                    }%`,
                  }}
                />
                <div
                  className="review-rate-rejected"
                  style={{
                    width: `${
                      (stats.reviewRate.rejected /
                        (stats.reviewRate.approved +
                          stats.reviewRate.rejected)) *
                      100
                    }%`,
                  }}
                />
              </>
            )}
          </div>
          <div className="review-rate-legend">
            <span className="legend-item">
              <span
                className="legend-color"
                style={{ backgroundColor: '#10b981' }}
              />{' '}
              Approved ({stats.reviewRate.approved})
            </span>
            <span className="legend-item">
              <span
                className="legend-color"
                style={{ backgroundColor: '#ef4444' }}
              />{' '}
              Rejected ({stats.reviewRate.rejected})
            </span>
          </div>
        </div>
      </section>
    </div>
  );
}
