import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// Mock global fetch
const mockFetch = vi.fn();
globalThis.fetch = mockFetch;

// We import after mocking fetch
import { searchMemories, getMemory, createMemory } from '../services/api';

describe('API service', () => {
  beforeEach(() => {
    mockFetch.mockReset();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('searchMemories', () => {
    it('calls POST /api/v2/search/semantic with the correct body', async () => {
      const mockResponse = {
        ok: true,
        status: 200,
        json: () =>
          Promise.resolve({
            results: [],
            total: 0,
            page: 1,
            pageSize: 10,
            totalPages: 0,
          }),
      };
      mockFetch.mockResolvedValueOnce(mockResponse);

      await searchMemories({ query: 'test query', topK: 10 });

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v2/search/semantic',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({ query: 'test query', topK: 10 }),
        })
      );
    });

    it('includes X-Tenant-Id header in the request', async () => {
      const mockResponse = {
        ok: true,
        status: 200,
        json: () =>
          Promise.resolve({
            results: [],
            total: 0,
            page: 1,
            pageSize: 10,
            totalPages: 0,
          }),
      };
      mockFetch.mockResolvedValueOnce(mockResponse);

      await searchMemories({ query: 'test' });

      const callArgs = mockFetch.mock.calls[0];
      const headers = callArgs[1].headers as Record<string, string>;
      expect(headers['X-Tenant-Id']).toBeDefined();
      expect(headers['X-Role']).toBeDefined();
    });

    it('throws ApiError on non-ok response', async () => {
      const mockResponse = {
        ok: false,
        status: 400,
        json: () =>
          Promise.resolve({
            errorCode: 'VALIDATION_ERROR',
            message: 'Invalid query',
          }),
      };
      mockFetch.mockResolvedValueOnce(mockResponse);

      await expect(
        searchMemories({ query: '' })
      ).rejects.toEqual({
        errorCode: 'VALIDATION_ERROR',
        message: 'Invalid query',
      });
    });
  });

  describe('getMemory', () => {
    it('calls GET /api/v2/memories/{id}', async () => {
      const mockMemory = {
        id: 'MEM-test',
        kind: 'fact',
        content: 'content',
        summary: 'summary',
        topics: [],
        entities: [],
        relations: [],
        metadata: {},
        source: { type: 'manual', ref: '' },
        scope: { tenantId: 't1' },
        lifecycle: {
          status: 'active',
          confidence: 0.5,
          importance: 0.5,
          sensitivity: 'internal',
          reviewedBy: null,
          reviewedAt: null,
        },
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      };

      const mockResponse = {
        ok: true,
        status: 200,
        json: () => Promise.resolve(mockMemory),
      };
      mockFetch.mockResolvedValueOnce(mockResponse);

      const result = await getMemory('MEM-test');

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v2/memories/MEM-test',
        expect.objectContaining({
          headers: expect.objectContaining({
            'X-Tenant-Id': 'tenant-001',
          }),
        })
      );
      expect(result.id).toBe('MEM-test');
    });
  });

  describe('createMemory', () => {
    it('calls POST /api/v2/memories with the correct body', async () => {
      const mockMemory = {
        id: 'MEM-new',
        kind: 'note',
        content: 'new content',
        summary: 'new summary',
        topics: [],
        entities: [],
        relations: [],
        metadata: {},
        source: { type: 'manual', ref: '' },
        scope: { tenantId: 'tenant-001' },
        lifecycle: {
          status: 'draft',
          confidence: 0.5,
          importance: 0.5,
          sensitivity: 'internal',
          reviewedBy: null,
          reviewedAt: null,
        },
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      };

      const mockResponse = {
        ok: true,
        status: 201,
        json: () => Promise.resolve(mockMemory),
      };
      mockFetch.mockResolvedValueOnce(mockResponse);

      const payload = {
        kind: 'note' as const,
        content: 'new content',
        summary: 'new summary',
        scope: { tenantId: 'tenant-001' },
      };

      const result = await createMemory(payload);

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v2/memories',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(payload),
        })
      );
      expect(result.id).toBe('MEM-new');
    });
  });

  describe('error handling', () => {
    it('handles non-JSON error responses gracefully', async () => {
      const mockResponse = {
        ok: false,
        status: 500,
        json: () => Promise.reject(new Error('Not JSON')),
        statusText: 'Internal Server Error',
      };
      mockFetch.mockResolvedValueOnce(mockResponse);

      await expect(getMemory('MEM-xxx')).rejects.toEqual({
        errorCode: 'UNKNOWN',
        message: 'HTTP 500: Internal Server Error',
      });
    });
  });
});
