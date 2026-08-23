package com.servando.homebudget.models.dto;

import com.servando.homebudget.models.database.BillCategory;

public record UpdateBillsRequestDto(String name, Double amount, int dueDay, BillCategory category) {
}
