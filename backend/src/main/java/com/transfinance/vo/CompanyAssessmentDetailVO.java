package com.transfinance.vo;

import java.util.List;

/**
 * 企业端单条评估数据详情。
 */
public class CompanyAssessmentDetailVO {

    /** 列表摘要及评估结论。 */
    private CompanyAssessmentRecordVO summary;

    /** 该行业记录的全部可展示字段。 */
    private List<FieldValue> fields;

    public CompanyAssessmentRecordVO getSummary() {
        return summary;
    }

    public void setSummary(CompanyAssessmentRecordVO summary) {
        this.summary = summary;
    }

    public List<FieldValue> getFields() {
        return fields;
    }

    public void setFields(List<FieldValue> fields) {
        this.fields = fields;
    }

    /**
     * 详情字段及其值。
     */
    public static class FieldValue {

        private String prop;
        private String label;
        private String type;
        private Object value;

        public FieldValue() {
        }

        public FieldValue(String prop, String label, String type, Object value) {
            this.prop = prop;
            this.label = label;
            this.type = type;
            this.value = value;
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

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }
}
