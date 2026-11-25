package com.transfinance.vo;

/**
 * 缺失字段展示对象。
 */
public class MissingColumnVO {

    private String prop;
    private String column;
    private String label;
    private String type;
    private Object oldValue;
    private Object newValue;

    public MissingColumnVO() {
    }

    public MissingColumnVO(String prop, String column, String label, String type) {
        this.prop = prop;
        this.column = column;
        this.label = label;
        this.type = type;
    }

    public String getProp() {
        return prop;
    }

    public void setProp(String prop) {
        this.prop = prop;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }
}
