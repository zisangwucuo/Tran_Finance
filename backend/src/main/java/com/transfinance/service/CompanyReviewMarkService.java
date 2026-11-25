package com.transfinance.service;

import com.transfinance.mapper.CompanyReviewMarkMapper;
import org.springframework.stereotype.Service;
import java.util.List;

/** 按当前登录机构持久化企业级标记，跨年份、历史与待评估页面共用。 */
@Service
public class CompanyReviewMarkService {
    private final CompanyReviewMarkMapper mapper;
    private boolean initialized;
    public CompanyReviewMarkService(CompanyReviewMarkMapper mapper) { this.mapper = mapper; }

    /** 兼容已有数据库，仅首次使用时建新增表，不重跑初始化SQL。 */
    private synchronized void initialize() {
        if (!initialized) { mapper.ensureTable(); initialized = true; }
    }
    public List<String> findCodes(String bank) { initialize(); return mapper.findCodes(bank); }
    public void mark(String bank, String code) { initialize(); mapper.mark(bank, code); }
}
