package com.transfinance.vo;

/**
 * 登录/注册成功后返回给前端的非敏感用户信息。
 */
public class LoginResponse {

    /** 金融机构用户ID。 */
    private Long id;

    /** 所属金融机构编码。 */
    private String institutionId;

    /** 所属金融机构名称。 */
    private String institutionName;

    /** 登录用户名。 */
    private String username;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(String institutionId) {
        this.institutionId = institutionId;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
