// Abax-Memory v2.0.0 — Shared Type Definitions
// English-only internals per BR-010

export type MemoryKind =
  | 'fact'
  | 'preference'
  | 'event'
  | 'decision'
  | 'task'
  | 'procedure'
  | 'note'
  | 'entity';

export type MemoryStatus =
  | 'draft'
  | 'pending'
  | 'active'
  | 'archived'
  | 'rejected'
  | 'deleted';

export type RelationType =
  | 'related_to'
  | 'depends_on'
  | 'caused_by'
  | 'resolves'
  | 'contradicts'
  | 'supports'
  | 'mentions'
  | 'belongs_to'
  | 'supersedes';

export type SensitivityLevel =
  | 'public'
  | 'internal'
  | 'confidential'
  | 'secret';

export type SourceType =
  | 'conversation'
  | 'document'
  | 'api'
  | 'workflow'
  | 'manual'
  | 'case';

export interface Scope {
  tenantId: string;
  userId?: string;
  sessionId?: string;
  namespace?: string;
}

export interface Lifecycle {
  status: MemoryStatus;
  confidence: number;
  importance: number;
  sensitivity: SensitivityLevel;
  reviewedBy: string | null;
  reviewedAt: string | null;
}

export interface MemorySource {
  type: SourceType;
  ref: string;
}

export interface MemoryRelation {
  id: string;
  targetId: string;
  type: RelationType;
}

export interface MemoryFragment {
  id: string;
  kind: MemoryKind;
  content: string;
  summary: string;
  topics: string[];
  entities: string[];
  relations: MemoryRelation[];
  metadata: Record<string, string>;
  source: MemorySource;
  scope: Scope;
  lifecycle: Lifecycle;
  createdAt: string;
  updatedAt: string;
}

export interface SearchFilters {
  kinds?: MemoryKind[];
  statuses?: MemoryStatus[];
  topics?: string[];
  entities?: string[];
  importance?: { gte?: number; lte?: number };
  confidence?: { gte?: number; lte?: number };
  sensitivities?: SensitivityLevel[];
  createdAfter?: string;
  createdBefore?: string;
}

export interface SearchRequest {
  query: string;
  filters?: SearchFilters;
  topK?: number;
  rerank?: boolean;
  expandGraph?: boolean;
  expandGraphDepth?: number;
  page?: number;
  pageSize?: number;
}

export interface SearchResult {
  memory: MemoryFragment;
  score: number;
}

export interface SearchResponse {
  results: SearchResult[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
  facets?: {
    kinds: Record<string, number>;
    statuses: Record<string, number>;
  };
}

export interface CreateMemoryRequest {
  kind: MemoryKind;
  content: string;
  summary?: string;
  topics?: string[];
  entities?: string[];
  metadata?: Record<string, string>;
  source?: MemorySource;
  scope: Scope;
  lifecycle?: {
    importance?: number;
    sensitivity?: SensitivityLevel;
    confidence?: number;
  };
}

export interface UpdateMemoryRequest {
  content?: string;
  summary?: string;
  topics?: string[];
  entities?: string[];
  metadata?: Record<string, string>;
  source?: MemorySource;
  lifecycle?: {
    importance?: number;
    sensitivity?: SensitivityLevel;
    confidence?: number;
    status?: MemoryStatus;
  };
}

export interface ReviewRequest {
  action: 'approve' | 'reject' | 'request_changes';
  comment?: string;
}

export interface GraphNode {
  memoryId: string;
  kind: MemoryKind;
  summary: string;
  status?: MemoryStatus;
}

export interface GraphEdge {
  sourceId: string;
  targetId: string;
  type: RelationType;
}

export interface GraphResponse {
  root: GraphNode;
  nodes: GraphNode[];
  edges: GraphEdge[];
  depth: number;
}

export interface RelationItem {
  id: string;
  targetId: string;
  type: RelationType;
}

export interface DomainProfile {
  id: string;
  name: string;
  kinds: MemoryKind[];
  defaultImportance: number;
  defaultSensitivity: SensitivityLevel;
  defaultConfidence: number;
  topics: string[];
}

export interface TenantConfig {
  tenantId: string;
  name: string;
  rateLimitPerMinute: number;
  activeProfileId: string | null;
}

export interface ApiError {
  errorCode: string;
  message: string;
  details?: { field: string; error: string }[];
}

export interface DashboardStats {
  totalFragments: number;
  byKind: Record<string, number>;
  byLifecycle: Record<string, number>;
  recentActivity: number;
  reviewRate: { approved: number; rejected: number };
}

// Lifecycle badge colors
export const LIFECYCLE_COLORS: Record<MemoryStatus, string> = {
  draft: '#6b7280',
  pending: '#f59e0b',
  active: '#10b981',
  archived: '#6b7280',
  rejected: '#ef4444',
  deleted: '#ef4444',
};

// Kind badge colors
export const KIND_COLORS: Record<MemoryKind, string> = {
  fact: '#3b82f6',
  preference: '#8b5cf6',
  event: '#f97316',
  decision: '#ec4899',
  task: '#14b8a6',
  procedure: '#6366f1',
  note: '#a3a3a3',
  entity: '#84cc16',
};

// Kind labels
export const KIND_LABELS: Record<MemoryKind, string> = {
  fact: 'Fact',
  preference: 'Preference',
  event: 'Event',
  decision: 'Decision',
  task: 'Task',
  procedure: 'Procedure',
  note: 'Note',
  entity: 'Entity',
};

// Sensitivity colors
export const SENSITIVITY_COLORS: Record<SensitivityLevel, string> = {
  public: '#10b981',
  internal: '#3b82f6',
  confidential: '#f59e0b',
  secret: '#ef4444',
};
