package com.transfinance.vo;

/**
 * 前端表格列元数据。
 */
public class ColumnMetaVO {

    /** 前端读取字段名。 */
    private String prop;

    /** 表头展示名称。 */
    private String label;

    /** 列宽。 */
    private Integer width;

    /** 数据类型：text/number/boolean。 */
    private String type;

    /** 固定列方向：left/right/null。 */
    private String fixed;

    public ColumnMetaVO() {
    }

    public ColumnMetaVO(String prop, String label, Integer width, String type, String fixed) {
        this.prop = prop;
        this.label = label;
        this.width = width;
        this.type = type;
        this.fixed = fixed;
    }

    public String getProp() {
        return prop;
    }

    public void setProp(String prop) {
        this.prop = prop;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFixed() {
        return fixed;
    }

    public void setFixed(String fixed) {
        this.fixed = fixed;
    }
}
