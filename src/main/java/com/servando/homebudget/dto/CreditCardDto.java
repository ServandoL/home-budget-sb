package com.servando.homebudget.dto;

public record CreditCardDto(
        String id,
        String name,
        Double currentBalance,
        Double minimumPayment,
        Double apr,
        Integer dueDay
) {
}
