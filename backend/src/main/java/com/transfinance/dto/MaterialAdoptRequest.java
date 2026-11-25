package com.transfinance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/** 指定目标行并逐项确认，expectedOldValue用于检测打开页面后的并发修改。 */
public record MaterialAdoptRequest(@NotNull @Positive Long rowId,
        boolean subjectConfirmed, @NotEmpty @Size(max = 12) List<@Valid Item> items) {
    public record Item(@NotBlank String field, @NotNull BigDecimal value,
            BigDecimal expectedOldValue, @NotBlank @Size(max = 500) String reviewNote) {}
}
