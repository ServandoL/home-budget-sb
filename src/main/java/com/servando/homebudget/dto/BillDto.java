package com.servando.homebudget.dto;

public record BillDto(
        String id,
        String name,
        Double amount,
        int dueDay,
        BillCategory category
) {
}
