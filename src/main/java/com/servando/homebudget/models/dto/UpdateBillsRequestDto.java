package com.servando.homebudget.models.dto;

import com.servando.homebudget.models.database.BillCategory;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

@Getter
public class UpdateBillsRequestDto extends SharedRequestProperties {
    @Nullable
    Double amount;
    @Nullable
    Integer dueDay;
    @Nullable
    BillCategory category;
}
