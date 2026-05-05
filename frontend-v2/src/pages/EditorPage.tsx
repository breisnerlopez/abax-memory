import { useState, useEffect, type FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import type { MemoryKind, SensitivityLevel, SourceType } from '../types';
import { KIND_LABELS } from '../types';
import {
  getMemory,
  createMemory,
  updateMemory,
} from '../services/api';

const ALL_KINDS: MemoryKind[] = [
  'fact',
  'preference',
  'event',
  'decision',
  'task',
  'procedure',
  'note',
  'entity',
];

const ALL_SENSITIVITIES: SensitivityLevel[] = [
  'public',
  'internal',
  'confidential',
  'secret',
];

const ALL_SOURCE_TYPES: SourceType[] = [
  'conversation',
  'document',
  'api',
  'workflow',
  'manual',
  'case',
];

interface FormData {
  title: string;
  content: string;
  kind: MemoryKind;
  sensitivity: SensitivityLevel;
  sourceType: SourceType;
  sourceRef: string;
  confidence: number;
  importance: number;
  topics: string;
}

const initialFormData: FormData = {
  title: '',
  content: '',
  kind: 'note',
  sensitivity: 'internal',
  sourceType: 'manual',
  sourceRef: '',
  confidence: 0.5,
  importance: 0.5,
  topics: '',
};

function parseTopics(input: string): string[] {
  return input
    .split(',')
    .map((t) => t.trim())
    .filter((t) => t.length > 0);
}

export default function EditorPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEdit = Boolean(id);

  const [form, setForm] = useState<FormData>(initialFormData);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!id) return;
    let cancelled = false;

    async function load() {
      setLoading(true);
      setLoadError(null);
      try {
        const mem = await getMemory(id!);
        if (!cancelled) {
          setForm({
            title: mem.summary || '',
            content: mem.content,
            kind: mem.kind,
            sensitivity: mem.lifecycle.sensitivity,
            sourceType: mem.source.type,
            sourceRef: mem.source.ref || '',
            confidence: mem.lifecycle.confidence,
            importance: mem.lifecycle.importance,
            topics: (mem.topics || []).join(', '),
          });
        }
      } catch (err: unknown) {
        if (!cancelled) {
          const apiErr = err as { message?: string };
          setLoadError(apiErr?.message || 'Failed to load memory for editing.');
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

  function validate(): boolean {
    const errs: Record<string, string> = {};

    if (!form.title.trim()) {
      errs.title = 'Title is required.';
    }
    if (!form.content.trim()) {
      errs.content = 'Content is required.';
    }
    if (form.confidence < 0 || form.confidence > 1) {
      errs.confidence = 'Confidence must be between 0 and 1.';
    }
    if (form.importance < 0 || form.importance > 1) {
      errs.importance = 'Importance must be between 0 and 1.';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitError(null);

    if (!validate()) return;

    setLoading(true);
    try {
      const payload = {
        kind: form.kind,
        content: form.content,
        summary: form.title,
        topics: parseTopics(form.topics),
        source: {
          type: form.sourceType,
          ref: form.sourceRef,
        },
        scope: {
          tenantId: 'tenant-001', // MOCK: from auth // REPLACE_BEFORE_PROD
        },
        lifecycle: {
          sensitivity: form.sensitivity,
          confidence: form.confidence,
          importance: form.importance,
        },
      };

      if (isEdit) {
        await updateMemory(id!, {
          content: payload.content,
          summary: payload.summary,
          topics: payload.topics,
          source: payload.source,
          lifecycle: {
            sensitivity: payload.lifecycle.sensitivity,
            confidence: payload.lifecycle.confidence,
            importance: payload.lifecycle.importance,
          },
        });
      } else {
        await createMemory(payload);
      }

      navigate('/');
    } catch (err: unknown) {
      const apiErr = err as { message?: string };
      setSubmitError(apiErr?.message || 'Failed to save memory.');
    } finally {
      setLoading(false);
    }
  }

  function setField<K extends keyof FormData>(
    field: K,
    value: FormData[K]
  ) {
    setForm((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => {
        const next = { ...prev };
        delete next[field];
        return next;
      });
    }
  }

  if (loading && isEdit) {
    return <div className="loading-spinner">Loading memory for editing...</div>;
  }

  if (loadError) {
    return (
      <div className="error-message" role="alert">
        {loadError}
        <button onClick={() => navigate(-1)}>Go back</button>
      </div>
    );
  }

  return (
    <div className="editor-page">
      <nav className="breadcrumb">
        <button onClick={() => navigate(-1)}>&larr; Back</button>
      </nav>

      <h2>{isEdit ? 'Edit Memory' : 'Create Memory'}</h2>

      {submitError && (
        <div className="error-message" role="alert">
          {submitError}
        </div>
      )}

      <form className="editor-form" onSubmit={handleSubmit} noValidate>
        <div className="form-field">
          <label htmlFor="title">Title *</label>
          <input
            id="title"
            type="text"
            value={form.title}
            onChange={(e) => setField('title', e.target.value)}
            placeholder="Memory title / summary"
            aria-invalid={!!errors.title}
          />
          {errors.title && (
            <span className="field-error">{errors.title}</span>
          )}
        </div>

        <div className="form-field">
          <label htmlFor="content">Content *</label>
          <textarea
            id="content"
            value={form.content}
            onChange={(e) => setField('content', e.target.value)}
            placeholder="Memory content (supports Markdown)"
            rows={8}
            aria-invalid={!!errors.content}
          />
          {errors.content && (
            <span className="field-error">{errors.content}</span>
          )}
        </div>

        <div className="form-row">
          <div className="form-field">
            <label htmlFor="kind">Kind</label>
            <select
              id="kind"
              value={form.kind}
              onChange={(e) => setField('kind', e.target.value as MemoryKind)}
              disabled={isEdit}
            >
              {ALL_KINDS.map((k) => (
                <option key={k} value={k}>
                  {KIND_LABELS[k]}
                </option>
              ))}
            </select>
            {isEdit && (
              <span className="field-hint">Kind is immutable after creation.</span>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="sensitivity">Sensitivity</label>
            <select
              id="sensitivity"
              value={form.sensitivity}
              onChange={(e) =>
                setField('sensitivity', e.target.value as SensitivityLevel)
              }
            >
              {ALL_SENSITIVITIES.map((s) => (
                <option key={s} value={s}>
                  {s.charAt(0).toUpperCase() + s.slice(1)}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="form-row">
          <div className="form-field">
            <label htmlFor="sourceType">Source Type</label>
            <select
              id="sourceType"
              value={form.sourceType}
              onChange={(e) =>
                setField('sourceType', e.target.value as SourceType)
              }
            >
              {ALL_SOURCE_TYPES.map((s) => (
                <option key={s} value={s}>
                  {s.charAt(0).toUpperCase() + s.slice(1)}
                </option>
              ))}
            </select>
          </div>

          <div className="form-field">
            <label htmlFor="sourceRef">Source Reference</label>
            <input
              id="sourceRef"
              type="text"
              value={form.sourceRef}
              onChange={(e) => setField('sourceRef', e.target.value)}
              placeholder="External reference ID"
            />
          </div>
        </div>

        <div className="form-row">
          <div className="form-field">
            <label htmlFor="confidence">
              Confidence: {(form.confidence * 100).toFixed(0)}%
            </label>
            <input
              id="confidence"
              type="range"
              min="0"
              max="1"
              step="0.05"
              value={form.confidence}
              onChange={(e) =>
                setField('confidence', parseFloat(e.target.value))
              }
            />
            {errors.confidence && (
              <span className="field-error">{errors.confidence}</span>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="importance">
              Importance: {(form.importance * 100).toFixed(0)}%
            </label>
            <input
              id="importance"
              type="range"
              min="0"
              max="1"
              step="0.05"
              value={form.importance}
              onChange={(e) =>
                setField('importance', parseFloat(e.target.value))
              }
            />
            {errors.importance && (
              <span className="field-error">{errors.importance}</span>
            )}
          </div>
        </div>

        <div className="form-field">
          <label htmlFor="topics">
            Topics (comma-separated)
          </label>
          <input
            id="topics"
            type="text"
            value={form.topics}
            onChange={(e) => setField('topics', e.target.value)}
            placeholder="e.g. deployment, kubernetes, incident"
          />
        </div>

        <div className="form-actions">
          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading}
          >
            {loading
              ? 'Saving...'
              : isEdit
                ? 'Update Memory'
                : 'Create Memory'}
          </button>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => navigate(-1)}
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
