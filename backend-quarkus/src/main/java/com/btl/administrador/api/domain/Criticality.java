package com.btl.administrador.api.domain;

public enum Criticality {
    BAJA,
    MEDIA,
    ALTA,
    CRITICA;

    public boolean requiresHumanApproval() {
        return this == ALTA || this == CRITICA;
    }
}
