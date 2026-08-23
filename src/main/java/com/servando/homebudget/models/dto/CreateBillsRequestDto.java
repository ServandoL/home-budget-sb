package com.servando.homebudget.models.dto;

import com.servando.homebudget.models.database.BillCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBillsRequestDto(
        @NotNull
        @NotBlank
        String name,
        @NotNull
        Double amount,
        int dueDay,
        @NotNull
        BillCategory category) {
}
