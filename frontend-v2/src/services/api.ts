// Abax-Memory v2.0.0 — API Client
// English-only internals per BR-010
// Uses fetch (no axios) to minimize dependencies
// MOCK: OIDC mock — X-Tenant-Id + X-Role headers // REPLACE_BEFORE_PROD

import type {
  MemoryFragment,
  SearchRequest,
  SearchResponse,
  CreateMemoryRequest,
  UpdateMemoryRequest,
  ReviewRequest,
  GraphResponse,
  RelationItem,
  DomainProfile,
  TenantConfig,
  DashboardStats,
  ApiError,
} from '../types';

const API_BASE = '/api/v2';

// MOCK: Hardcoded tenant/role for development // REPLACE_BEFORE_PROD
const MOCK_TENANT_ID = 'tenant-001';
const MOCK_ROLE = 'memory-admin';

function headers(): Record<string, string> {
  return {
    'Content-Type': 'application/json',
    'X-Tenant-Id': MOCK_TENANT_ID,
    'X-Role': MOCK_ROLE,
  };
}

async function request<T>(
  url: string,
  options: RequestInit = {}
): Promise<T> {
  const response = await fetch(url, {
    ...options,
    headers: {
      ...headers(),
      ...(options.headers || {}),
    },
  });

  if (!response.ok) {
    let apiError: ApiError;
    try {
      apiError = await response.json();
    } catch {
      apiError = {
        errorCode: 'UNKNOWN',
        message: `HTTP ${response.status}: ${response.statusText}`,
      };
    }
    throw apiError;
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

// Memory CRUD
export async function getMemories(params?: {
  page?: number;
  pageSize?: number;
  status?: string;
}): Promise<SearchResponse> {
  const searchParams = new URLSearchParams();
  if (params?.page) searchParams.set('page', String(params.page));
  if (params?.pageSize) searchParams.set('pageSize', String(params.pageSize));
  if (params?.status) searchParams.set('status', params.status);
  const query = searchParams.toString();
  return request<SearchResponse>(
    `${API_BASE}/memories${query ? `?${query}` : ''}`
  );
}

export async function getMemory(id: string): Promise<MemoryFragment> {
  return request<MemoryFragment>(`${API_BASE}/memories/${id}`);
}

export async function createMemory(
  data: CreateMemoryRequest
): Promise<MemoryFragment> {
  return request<MemoryFragment>(`${API_BASE}/memories`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function updateMemory(
  id: string,
  data: UpdateMemoryRequest
): Promise<MemoryFragment> {
  return request<MemoryFragment>(`${API_BASE}/memories/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function deleteMemory(id: string): Promise<void> {
  return request<void>(`${API_BASE}/memories/${id}`, {
    method: 'DELETE',
  });
}

// Search
export async function searchMemories(
  searchRequest: SearchRequest
): Promise<SearchResponse> {
  return request<SearchResponse>(`${API_BASE}/search/semantic`, {
    method: 'POST',
    body: JSON.stringify(searchRequest),
  });
}

// Relations
export async function getRelations(memoryId: string): Promise<RelationItem[]> {
  return request<RelationItem[]>(`${API_BASE}/relations/${memoryId}`);
}

export async function createRelation(
  sourceId: string,
  targetId: string,
  type: string
): Promise<RelationItem> {
  return request<RelationItem>(`${API_BASE}/relations`, {
    method: 'POST',
    body: JSON.stringify({ sourceId, targetId, type }),
  });
}

export async function deleteRelation(relationId: string): Promise<void> {
  return request<void>(`${API_BASE}/relations/${relationId}`, {
    method: 'DELETE',
  });
}

// Graph
export async function expandGraph(
  memoryId: string,
  depth: number = 2
): Promise<GraphResponse> {
  return request<GraphResponse>(
    `${API_BASE}/memories/${memoryId}/graph?depth=${depth}`
  );
}

// Review
export async function reviewMemory(
  memoryId: string,
  review: ReviewRequest
): Promise<MemoryFragment> {
  return request<MemoryFragment>(
    `${API_BASE}/memories/${memoryId}/review`,
    {
      method: 'POST',
      body: JSON.stringify(review),
    }
  );
}

// Admin
export async function getTenantConfig(): Promise<TenantConfig> {
  return request<TenantConfig>(`${API_BASE}/admin/tenant`);
}

export async function getProfiles(): Promise<DomainProfile[]> {
  return request<DomainProfile[]>(`${API_BASE}/admin/profiles`);
}

export async function reindex(): Promise<{ message: string }> {
  return request<{ message: string }>(`${API_BASE}/admin/reindex`, {
    method: 'POST',
  });
}

// Dashboard / Stats
export async function getDashboardStats(): Promise<DashboardStats> {
  return request<DashboardStats>(`${API_BASE}/memories/stats`);
}

// Entity extraction (utility)
export async function extractEntities(content: string): Promise<string[]> {
  const response = await request<{ entities: { name: string }[] }>(
    `${API_BASE}/memories/extract`,
    {
      method: 'POST',
      body: JSON.stringify({ content }),
    }
  );
  return response.entities.map((e) => e.name);
}
