package com.servando.homebudget.models.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class UpdateEventBudgetDto extends SharedRequestProperties {
    @Nullable
    @Positive
    @DecimalMax("10000000")
    Double budget;
    /** YYYY/MM/DD, as the app stores dates; an empty string clears it. */
    @Nullable
    @Pattern(regexp = "(\\d{4}/\\d{2}/\\d{2})?")
    String date;
    /** True marks it done, false reopens it. */
    @Nullable
    Boolean done;
}
