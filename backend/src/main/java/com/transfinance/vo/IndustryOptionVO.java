package com.transfinance.vo;

/**
 * 行业下拉选项。
 */
public class IndustryOptionVO {

    /** 前端使用的行业键。 */
    private String key;

    /** 行业名称。 */
    private String name;

    public IndustryOptionVO() {
    }

    public IndustryOptionVO(String key, String name) {
        this.key = key;
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
