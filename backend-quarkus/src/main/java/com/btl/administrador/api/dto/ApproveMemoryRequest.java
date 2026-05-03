package com.btl.administrador.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ApproveMemoryRequest(@NotBlank String comentario) {
}
