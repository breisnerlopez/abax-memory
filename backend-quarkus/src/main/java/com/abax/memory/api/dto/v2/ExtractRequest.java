package com.abax.memory.api.dto.v2;

import com.abax.memory.domain.enums.MemoryKind;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Request DTO for POST /api/v2/memories/extract — entity extraction.
 *
 * <p>New in v2.1.0: {@code domain} field to guide domain-specific extraction.
 * {@code kind} is retained for backward compatibility.</p>
 *
 * <p>{@code content} is the canonical field name. The legacy field name
 * {@code "text"} is accepted as an alias via {@code setText(String)} which
 * delegates to {@link #setContent(String)}. This works with Jackson
 * setter-based deserialization (unlike {@code @JsonAlias} on records which
 * is not supported for record constructor-based deserialization in
 * RESTEasy Reactive / Jackson).</p>
 *
 * <p>References: HU-005.8.1, FT-V21-001.4, DEF-V21-007, DEF-V21-010</p>
 */
public class ExtractRequest {

    @NotBlank(message = "content is required")
    @Size(max = 5000, message = "content exceeds maximum length of 5000 characters")
    private String content;

    private MemoryKind kind;

    private String domain;

    // ── Constructors ─────────────────────────────────────────────

    public ExtractRequest() {
    }

    public ExtractRequest(String content, MemoryKind kind, String domain) {
        this.content = content;
        this.kind = kind;
        this.domain = domain;
    }

    // ── Getters ──────────────────────────────────────────────────

    public String getContent() {
        return content;
    }

    public MemoryKind getKind() {
        return kind;
    }

    public String getDomain() {
        return domain;
    }

    // ── Setters ──────────────────────────────────────────────────

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Alias for {@link #setContent(String)} — accepts {@code "text"} as
     * a JSON field name for backward compatibility with API consumers
     * who use the legacy field name.
     *
     * <p>DEF-V21-010: Jackson's {@code @JsonAlias} on record components
     * does not work with constructor-based deserialization. Using an
     * explicit setter with {@code @JsonSetter} provides reliable alias
     * support with Jackson's setter-based deserialization.</p>
     */
    @JsonSetter("text")
    public void setText(String text) {
        this.content = text;
    }

    public void setKind(MemoryKind kind) {
        this.kind = kind;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    // ── Object contract ──────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExtractRequest that)) return false;
        return Objects.equals(content, that.content)
                && kind == that.kind
                && Objects.equals(domain, that.domain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, kind, domain);
    }

    @Override
    public String toString() {
        return "ExtractRequest{"
                + "content='" + (content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content) + '\''
                + ", kind=" + kind
                + ", domain='" + domain + '\''
                + '}';
    }
}
