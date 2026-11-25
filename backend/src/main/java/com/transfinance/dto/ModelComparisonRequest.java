package com.transfinance.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;

/** 实验范围白名单；机构由服务端登录会话确定，不能由客户端指定其他银行。 */
public record ModelComparisonRequest(
        @NotNull @Pattern(regexp="all|power|steel|chemical|petrochemical|building|nonferrous|coal|aviation") String industryKey,
        @Pattern(regexp="default|tuned|optimized") String mode) {
    /** 兼容旧客户端：未传模式时仍采用固定基线。 */
    public ModelComparisonRequest(String industryKey) { this(industryKey, "default"); }
    public String effectiveMode() { return mode == null ? "default" : mode; }
}
