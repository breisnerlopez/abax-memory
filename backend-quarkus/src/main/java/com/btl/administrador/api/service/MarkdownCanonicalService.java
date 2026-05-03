package com.btl.administrador.api.service;

import com.btl.administrador.api.domain.MemoryRecord;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.StringJoiner;

@ApplicationScoped
public class MarkdownCanonicalService {

    public String render(MemoryRecord memoryRecord, String body) {
        StringJoiner yaml = new StringJoiner("\n");
        yaml.add("---");
        yaml.add("id: " + memoryRecord.id);
        yaml.add("title: \"" + sanitize(memoryRecord.title) + "\"");
        yaml.add("type: " + memoryRecord.type);
        yaml.add("origin: " + memoryRecord.origin.name().toLowerCase());
        yaml.add("criticality: " + memoryRecord.criticality.name().toLowerCase());
        yaml.add("state: " + memoryRecord.state.name().toLowerCase());
        yaml.add("domains: [" + String.join(", ", memoryRecord.domains) + "]");
        yaml.add("tags: [" + String.join(", ", memoryRecord.tags) + "]");
        if (memoryRecord.sourceCaseId != null) {
            yaml.add("sourceCaseId: " + memoryRecord.sourceCaseId);
        }
        memoryRecord.metadata.forEach((key, value) -> yaml.add(key + ": \"" + sanitize(value) + "\""));
        yaml.add("---");
        return yaml + "\n" + body.trim() + "\n";
    }

    private String sanitize(String input) {
        return input == null ? "" : input.replace("\"", "'");
    }
}
