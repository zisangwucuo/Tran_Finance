package com.transfinance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.transfinance.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** 使用本机模拟AI供应商验证真实HTTP协议，不发送企业材料到外网。 */
class MaterialAiClientTest {
    @Test void normalizesCommonWrappersWithoutInventingFacts() throws Exception {
        var json=new ObjectMapper();
        var client=new MaterialAiClient(json,false,"","","",false,false);
        assertTrue(client.normalize(json.readTree("[]")).path("candidates").isArray());
        assertTrue(client.normalize(json.readTree("{\"data\":{\"candidates\":[]}}" )).path("candidates").isArray());
        assertThrows(BusinessException.class,()->client.normalize(json.createObjectNode()));
    }
    @Test void retriesUnsupportedJsonFormatAndParsesResponse() throws Exception {
        ObjectMapper json=new ObjectMapper(); AtomicInteger count=new AtomicInteger();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/chat", exchange->{
            var body=json.readTree(exchange.getRequestBody());
            int n=count.incrementAndGet();
            if(n==1) { assertTrue(body.has("response_format")); exchange.sendResponseHeaders(400,-1); }
            else {
                assertFalse(body.has("response_format"));
                byte[] response=json.writeValueAsBytes(Map.of("choices",List.of(Map.of("finish_reason","stop",
                        "message",Map.of("content","{\"candidates\":[]}")))));
                exchange.getResponseHeaders().set("Content-Type","application/json");
                exchange.sendResponseHeaders(200,response.length); exchange.getResponseBody().write(response);
            }
            exchange.close();
        });
        server.start();
        try {
            var client=new MaterialAiClient(json,true,"http://127.0.0.1:"+server.getAddress().getPort()+"/chat",
                    UUID.randomUUID().toString(),"test-model",false,true);
            assertTrue(client.extract("synthetic text").path("candidates").isArray()); assertEquals(2,count.get());
        } finally { server.stop(0); }
    }
    @Test void missingConfigurationNeverReturnsFakeValues() {
        var client=new MaterialAiClient(new ObjectMapper(),false,"","","",false,false);
        assertFalse(client.configured()); assertThrows(BusinessException.class,()->client.extract("text"));
    }
}
