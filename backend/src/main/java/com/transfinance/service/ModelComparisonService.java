package com.transfinance.service;

import com.transfinance.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.util.Map;

/** 实验代理服务。数据由Python复用既有只读查询，结果独立保存，不增加业务表写入。 */
@Service
public class ModelComparisonService {
    private final RestClient client;
    private final String url;
    public ModelComparisonService(RestClient client, @Value("${ml.service-url:http://127.0.0.1:8090}") String url) {
        this.client=client; this.url=url;
    }
    public Object list(String bank) {
        try {
            return client.get().uri(url+"/ml/comparisons?institutionId={bank}",bank).retrieve().body(Object.class);
        } catch(RestClientException e) { throw new BusinessException("对比实验服务不可用，请重启最新版Python模型服务"); }
    }
    public Object start(String bank,String industry,String mode) {
        try {
            return client.post().uri(url+"/ml/comparisons").body(Map.of("institutionId",bank,"industryKey",industry,"mode",mode))
                    .retrieve().body(Object.class);
        } catch(RestClientResponseException e) {
            if(e.getStatusCode().value()==409) throw new BusinessException("当前机构已有对比实验正在运行");
            throw new BusinessException("实验启动失败，请检查Python模型服务版本和状态");
        } catch(RestClientException e) { throw new BusinessException("对比实验服务不可用，请启动Python模型服务"); }
    }
}
