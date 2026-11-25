package com.transfinance.dto;

import jakarta.validation.constraints.*;

/** 随机森林可调参数；限制计算规模，拒绝空值和未支持的 sklearn 选项。 */
public record ModelParameters(
        @NotNull @Min(50) @Max(1000) Integer nEstimators,
        @NotNull @Min(1) @Max(30) Integer maxDepth,
        @NotNull @Min(1) @Max(50) Integer minSamplesLeaf,
        @NotNull @Min(2) @Max(100) Integer minSamplesSplit,
        @NotNull @Pattern(regexp = "sqrt|log2|all") String maxFeatures,
        @NotNull @Pattern(regexp = "balanced|balanced_subsample|none") String classWeight,
        @NotNull @Min(0) @Max(2147483647) Integer randomState
) {
    /** 一键训练始终使用此基线，不受更新弹窗的修改影响。 */
    public static ModelParameters defaults() {
        return new ModelParameters(300, 8, 2, 2, "sqrt", "balanced", 42);
    }
}
