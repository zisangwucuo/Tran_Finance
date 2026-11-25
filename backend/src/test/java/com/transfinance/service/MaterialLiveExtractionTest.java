package com.transfinance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transfinance.support.MaterialIndicatorCatalog;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 显式开启的真实AI验证，只发送指定报告第12页，不访问或修改数据库，不输出密钥/材料。 */
@EnabledIfSystemProperty(named="material.live", matches="true")
class MaterialLiveExtractionTest {
    @Test void extractProvidedReportPage() throws Exception {
        Path config=Path.of("src/main/resources/application-local.yml");
        Assumptions.assumeTrue(Files.isRegularFile(config),"Local AI configuration unavailable");
        var loader=new YamlPropertiesFactoryBean(); loader.setResources(new FileSystemResource(config));
        var props=loader.getObject();
        String url=setting(props,"MATERIAL_AI_API_URL","api-url");
        String key=setting(props,"MATERIAL_AI_API_KEY","api-key");
        String model=setting(props,"MATERIAL_AI_MODEL","model");
        Assumptions.assumeTrue(!url.isBlank() && !key.isBlank() && !model.isBlank(),"AI credentials not available to test process");
        String sample=System.getProperty("material.live.pdf");
        Assumptions.assumeTrue(sample!=null,"A report must be explicitly specified");
        var page=new MaterialPdfReader().read(Path.of(sample)).get(11);
        var pipeline=new MaterialFactPipeline();
        var ai=new MaterialAiClient(new ObjectMapper(),true,url,key,model,
                Boolean.parseBoolean(setting(props,"MATERIAL_AI_THINKING_DISABLED","deepseek-thinking-disabled")),false);
        var response=ai.extract(pipeline.prompt(List.of(page)));
        var facts=pipeline.facts(List.of(response));
        assertFalse(facts.isEmpty(),"Live AI returned no facts for a page with quantitative indicators");
        var results=pipeline.match(facts,MaterialIndicatorCatalog.forIndustry("steel"),List.of(page),2025,
                "演示账号（不允许入库）",new MaterialCandidateValidator());
        assertTrue(results.stream().noneMatch(c->Set.of("READY","REVIEW").contains(c.status())));
        System.out.println("Live AI verified: facts="+facts.size()+", retained="+results.stream().filter(c->!"MISSING".equals(c.status())).count());
    }
    private String setting(Properties properties,String env,String suffix) {
        String value=System.getenv().getOrDefault(env,properties.getProperty("ai.narrative."+suffix,""));
        return value.startsWith("${")?"":value;
    }
}
