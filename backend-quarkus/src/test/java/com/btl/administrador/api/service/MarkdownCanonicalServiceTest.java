package com.btl.administrador.api.service;

import com.btl.administrador.api.domain.Criticality;
import com.btl.administrador.api.domain.MemoryOrigin;
import com.btl.administrador.api.domain.MemoryRecord;
import com.btl.administrador.api.domain.MemoryState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownCanonicalServiceTest {

    @Test
    void render_includesFrontmatterAndSanitizesQuotes() {
        MarkdownCanonicalService service = new MarkdownCanonicalService();
        MemoryRecord memory = new MemoryRecord();
        memory.id = "MEM-123";
        memory.title = "Guia \"oficial\"";
        memory.type = "RUNBOOK";
        memory.origin = MemoryOrigin.CASO;
        memory.criticality = Criticality.MEDIA;
        memory.state = MemoryState.EN_REVISION;
        memory.domains = List.of("RRHH", "OPS");
        memory.tags = List.of("guia", "mvp");
        memory.sourceCaseId = "CASO-001";
        memory.metadata = Map.of("fuente", "jira", "autor", "ana \"lopez\"");

        String markdown = service.render(memory, "  # cuerpo principal  ");

        assertThat(markdown).contains("---");
        assertThat(markdown).contains("id: MEM-123");
        assertThat(markdown).contains("title: \"Guia 'oficial'\"");
        assertThat(markdown).contains("origin: caso");
        assertThat(markdown).contains("criticality: media");
        assertThat(markdown).contains("state: en_revision");
        assertThat(markdown).contains("domains: [RRHH, OPS]");
        assertThat(markdown).contains("tags: [guia, mvp]");
        assertThat(markdown).contains("sourceCaseId: CASO-001");
        assertThat(markdown).contains("autor: \"ana 'lopez'\"");
        assertThat(markdown).endsWith("# cuerpo principal\n");
    }
}
