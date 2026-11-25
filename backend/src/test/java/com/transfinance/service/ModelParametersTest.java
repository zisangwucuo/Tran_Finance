package com.transfinance.service;

import com.transfinance.dto.ModelParameters;
import com.transfinance.dto.ModelTrainingRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 验证嵌套请求确实执行参数校验，防止非法参数被提交给后台训练任务。 */
class ModelParametersTest {
    @Test
    void validatesNestedParameters() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = new ModelTrainingRequest();
            request.setInstitutionId("test");
            assertTrue(validator.validate(request).isEmpty());
            request.setParameters(new ModelParameters(0, 31, null, 1, "bad", "bad", -1));
            assertEquals(7, validator.validate(request).size());
        }
    }
}
