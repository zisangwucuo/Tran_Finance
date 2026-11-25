package com.transfinance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transfinance.exception.BusinessException;
import com.transfinance.support.IndustryDataCatalog;
import com.transfinance.vo.CommonCustomerVO;
import org.springframework.stereotype.Service;
import java.nio.file.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** 隐私求交网关：只访问本行节点，不读取跨机构明细，也不透传节点凭据。 */
@Service
public class CommonCustomerService {
    private final ObjectMapper json;
    // Uvicorn节点使用HTTP/1.1，禁用JDK默认h2c升级以避免POST请求被拒绝。
    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(3)).build();
    public CommonCustomerService(ObjectMapper json) { this.json = json; }
    public record Result(List<CommonCustomerVO> records, int skippedLocalRows,
                         int localCustomerYears, List<String> checkedPeers) {}
    public record NodeStatus(String institutionId, String status) {}

    /** 启动时无需配置PSI；使用功能时才读取运行目录，避免影响其他业务启动。 */
    private Path configPath() {
        String configured = System.getenv("PSI_GATEWAY_CONFIG");
        if (configured != null && !configured.isBlank()) return Path.of(configured);
        for (Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath(); p != null; p = p.getParent()) {
            Path file = p.resolve(".runtime/psi/gateway.json");
            if (Files.isRegularFile(file)) return file;
        }
        throw new BusinessException("隐私求交节点尚未配置，请先初始化并启动三个银行节点");
    }
    private com.fasterxml.jackson.databind.JsonNode nodes() {
        try { return json.readTree(configPath().toFile()).required("nodes"); }
        catch (BusinessException e) { throw e; }
        catch (Exception e) { throw new BusinessException("隐私求交配置无法读取"); }
    }
    private String call(String bank, String path, String body, int seconds) throws Exception {
        var node = nodes().get(bank);
        if (node == null) throw new BusinessException("当前机构尚未配置隐私求交节点");
        URI uri = URI.create(node.required("url").asText() + path);
        if (!"http".equals(uri.getScheme()) || !Set.of("localhost", "127.0.0.1").contains(uri.getHost()))
            throw new BusinessException("节点地址必须为本机地址");
        var builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(seconds))
                .header("Authorization", "Bearer " + node.required("token").asText());
        if (body == null) builder.GET();
        else builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new BusinessException("银行节点处理失败，请检查三个节点状态后重试");
        return response.body();
    }
    public Result find(String bank, String industry, String dataset) {
        IndustryDataCatalog.requireIndustry(industry);
        if (dataset == null || !Set.of("history", "pending").contains(dataset))
            throw new BusinessException("数据范围无效");
        try {
            String body = json.writeValueAsString(Map.of("industry", industry, "dataset", dataset));
            Result result = json.readValue(call(bank, "/run", body, 200), Result.class);
            // 字段名来自本地行业白名单；只返回差异名称，不返回任何对方取值。
            Map<String, String> labels = new HashMap<>();
            labels.put("company_type", "企业类型");
            IndustryDataCatalog.requireIndustry(industry).featureFields()
                    .forEach(field -> labels.put(field.column(), field.label()));
            List<CommonCustomerVO> records = result.records().stream().map(row -> {
                if (row.differentFields() == null || row.differentFields().stream().anyMatch(f -> !labels.containsKey(f)))
                    throw new BusinessException("节点字段版本不一致，请重新启动三个银行节点");
                return new CommonCustomerVO(row.companyCode(), row.companyName(), row.year(),
                        row.otherInstitution(), row.otherInstitutionName(), row.status(), row.differentFields(),
                        row.differentFields().stream().map(labels::get).toList());
            }).toList();
            return new Result(records, result.skippedLocalRows(), result.localCustomerYears(), result.checkedPeers());
        } catch (BusinessException e) { throw e; }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("隐私求交请求已中断");
        } catch (Exception e) { throw new BusinessException("隐私求交节点不可用，请确认三个节点已启动"); }
    }
    /** 离线只返回状态，不输出含敏感配置的底层异常。 */
    public List<NodeStatus> status() {
        List<NodeStatus> result = new ArrayList<>();
        for (String bank : List.of("bank_a", "bank_b", "bank_c")) {
            String state = "OFFLINE";
            try {
                if (!nodes().has(bank)) state = "NOT_CONFIGURED";
                else {
                    var response = json.readTree(call(bank, "/health", null, 3));
                    if (bank.equals(response.path("institutionId").asText())
                            && "READY".equals(response.path("status").asText())
                            && "MPC4J-1.1.5-HFH99-ECC-FIELDS-v2".equals(response.path("protocol").asText())) state = "READY";
                }
            } catch (BusinessException e) { state = "UNAVAILABLE"; }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            catch (Exception ignored) { /* 连接失败按离线展示。 */ }
            result.add(new NodeStatus(bank, state));
        }
        return result;
    }
}
