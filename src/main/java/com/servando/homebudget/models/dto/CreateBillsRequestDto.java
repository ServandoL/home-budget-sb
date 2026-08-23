package com.servando.homebudget.models.dto;

import com.servando.homebudget.models.database.BillCategory;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CreateBillsRequestDto extends SharedRequestProperties {
        @NotNull
        Double amount;
        @Nullable
        Integer dueDay;
        @NotNull
        BillCategory category;
}
