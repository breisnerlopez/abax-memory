package com.btl.administrador.api.exception;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CorrelationIdHolder {

    private final ThreadLocal<String> correlationId = new ThreadLocal<>();

    public String getCorrelationId() {
        return correlationId.get();
    }

    public void setCorrelationId(String value) {
        correlationId.set(value);
    }

    public void clear() {
        correlationId.remove();
    }
}
