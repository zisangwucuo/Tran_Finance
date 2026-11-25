package com.transfinance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 客户总览的筛选条件；机构身份不属于可传入参数，由登录会话提供。 */
public record CustomerOverviewQueryRequest(@Min(2000) @Max(2100) Integer dataYear) { }
