package com.transfinance.service;

import com.transfinance.dto.CompanyProfileUpdateRequest;
import com.transfinance.dto.CompanyRegisterRequest;
import com.transfinance.dto.LoginRequest;
import com.transfinance.vo.CompanyLoginResponse;
import com.transfinance.vo.CompanyProfileVO;

/**
 * 企业端认证与资料服务。
 */
public interface CompanyAuthService {

    CompanyLoginResponse login(LoginRequest request);

    CompanyLoginResponse register(CompanyRegisterRequest request);

    CompanyProfileVO profile(String companyCode);

    CompanyProfileVO updateProfile(CompanyProfileUpdateRequest request);
}
