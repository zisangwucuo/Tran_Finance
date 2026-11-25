package com.transfinance.service;

import com.transfinance.vo.CustomerOverviewVO;

/** 当前登录金融机构的客户全量统计，不触发训练、补全通知或任何数据写入。 */
public interface CustomerOverviewService {
    CustomerOverviewVO overview(String institutionCode, Integer dataYear);
}
