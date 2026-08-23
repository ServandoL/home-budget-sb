package com.servando.homebudget.dto;

public record IncomeDto(
        Double netAmount,
        PayFrequency frequency
) {
}
