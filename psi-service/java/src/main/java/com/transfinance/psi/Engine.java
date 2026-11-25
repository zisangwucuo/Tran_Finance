package com.transfinance.psi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryParty;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pso.psi.pke.hfh99.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Windows原生PSI引擎。密码学运算完整委托给MPC4J的HFH99实现。
 * 本类仅将库的RPC数据包转换成管道JSON；本行输入通过标准输入传入，
 * 不使用命令行参数、临时明文文件或日志传递企业记录。
 */
public final class Engine {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final BufferedReader IN = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    private static final PrintWriter OUT = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
    private static final int LIMIT = 100000;

    private static JsonNode read() throws IOException {
        String line = IN.readLine();
        if (line == null || line.length() > 32 * 1024 * 1024) throw new IOException("Invalid input");
        return JSON.readTree(line);
    }
    private static void write(Object value) {
        try { OUT.println(JSON.writeValueAsString(value)); }
        catch (IOException e) { throw new IllegalStateException("Pipe failure"); }
    }

    public static void main(String[] args) {
        try {
            JsonNode init = read();
            boolean client = "client".equals(init.path("role").asText());
            if (!client && !"server".equals(init.path("role").asText())) throw new IllegalArgumentException();
            Set<String> values = new LinkedHashSet<>();
            for (JsonNode value : init.required("values")) {
                if (!value.isTextual() || value.asText().length() > 65536) throw new IllegalArgumentException();
                values.add(value.asText());
            }
            int peerSize = init.required("peerCount").asInt();
            if (values.isEmpty() || values.size() > LIMIT || peerSize < 1 || peerSize > LIMIT) throw new IllegalArgumentException();
            PipeRpc rpc = new PipeRpc(client, init);
            var config = new Hfh99EccPsiConfig.Builder().build();
            // 必须在协议构造之前指定STANDARD_JDK，禁止默认环境选择OpenSSL。
            config.setEnvType(EnvType.STANDARD_JDK);
            if (client) {
                var protocol = new Hfh99EccPsiClient<String>(rpc, rpc.getParty(1), config);
                protocol.init(values.size(), peerSize);
                write(Map.of("matches", protocol.psi(values, peerSize)));
                protocol.destroy();
            } else {
                var protocol = new Hfh99EccPsiServer<String>(rpc, rpc.getParty(0), config);
                protocol.init(values.size(), peerSize);
                protocol.psi(values, peerSize);
                write(Map.of("packets", rpc.sent));
                protocol.destroy();
            }
        } catch (Throwable failure) {
            // 不输出底层异常内容，避免库异常包含输入、密钥或协议消息。
            System.err.println("PSI engine failed: " + failure.getClass().getSimpleName());
            System.exit(1);
        }
    }

    /**
     * 有序单次会话的传输适配器。HFH99客户端发送一个包，服务端返回两个包。
     * 网络鉴权、超时和请求范围在外层银行节点处理；这里不实现密码学算法。
     */
    private static final class PipeRpc implements Rpc {
        private final Party a = new MemoryParty(0, "receiver"), b = new MemoryParty(1, "sender");
        private final boolean client;
        private final List<List<String>> sent = new ArrayList<>();
        private final Queue<List<byte[]>> incoming = new ArrayDeque<>();
        PipeRpc(boolean client, JsonNode init) {
            this.client = client;
            if (!client) incoming.add(decode(init.required("encrypted")));
        }
        private List<byte[]> decode(JsonNode packet) {
            if (!packet.isArray() || packet.size() > LIMIT) throw new IllegalArgumentException();
            List<byte[]> result = new ArrayList<>();
            for (JsonNode value : packet) {
                if (!value.isTextual() || value.asText().length() > 256) throw new IllegalArgumentException();
                result.add(Base64.getDecoder().decode(value.asText()));
            }
            return result;
        }
        public Party ownParty() { return client ? a : b; }
        public Set<Party> getPartySet() { return Set.of(a, b); }
        public Party getParty(int id) {
            if (id != 0 && id != 1) throw new IllegalArgumentException();
            return id == 0 ? a : b;
        }
        public void send(DataPacket packet) {
            List<String> payload = packet.getPayload().stream().map(Base64.getEncoder()::encodeToString).toList();
            if (client) write(Map.of("encrypted", payload));
            else sent.add(payload);
        }
        public DataPacket receive(DataPacketHeader header) {
            if (incoming.isEmpty() && client) {
                try {
                    JsonNode packets = read().required("packets");
                    if (!packets.isArray() || packets.size() != 2) throw new IllegalArgumentException();
                    for (JsonNode packet : packets) incoming.add(decode(packet));
                } catch (IOException e) { throw new IllegalStateException("Pipe failure"); }
            }
            if (incoming.isEmpty()) throw new IllegalStateException("Missing protocol packet");
            return DataPacket.fromByteArrayList(header, incoming.remove());
        }
        public DataPacket receiveAny(int protocolId) { throw new UnsupportedOperationException(); }
        public void connect() { }
        public void disconnect() { }
        public void synchronize() { throw new UnsupportedOperationException(); }
        public void reset() { }
        public long getPayloadByteLength() { return 0; }
        public long getSendByteLength() { return 0; }
        public long getSendDataPacketNum() { return sent.size(); }
    }
}
