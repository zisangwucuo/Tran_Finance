package com.transfinance.service;

import com.transfinance.vo.AssessmentReportVO;

import java.util.List;

/**
 * 评估结论与改进路径生成服务。
 */
public interface AssessmentNarrativeService {

    /**
     * 根据报告结构化指标生成文本解读。
     *
     * @param report 当前报告主体
     * @param allIndicators 所有指标得分与贡献值
     * @return 结论与建议
     */
    AssessmentReportVO.Narrative generate(AssessmentReportVO report, List<AssessmentReportVO.IndicatorItem> allIndicators);
}
