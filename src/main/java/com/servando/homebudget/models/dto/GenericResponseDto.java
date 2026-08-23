package com.servando.homebudget.models.dto;

public record GenericResponseDto<T>(
        Boolean success,
        String message,
        T data
) {
}
