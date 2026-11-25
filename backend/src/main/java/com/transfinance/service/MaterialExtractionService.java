package com.transfinance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transfinance.dto.MaterialAdoptRequest;
import com.transfinance.entity.Company;
import com.transfinance.exception.BusinessException;
import com.transfinance.mapper.*;
import com.transfinance.support.*;
import com.transfinance.vo.MaterialExtractionVO;
import com.transfinance.vo.MaterialExtractionVO.*;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

/**
 * 企业材料工作流：文件留存、受限后台队列、证据提取、人工确认与事务审计。
 * 仅单实例演示部署；任务重启标为失败，已成功的任务从数据库恢复展示。
 */
@Service
public class MaterialExtractionService {
    private final MaterialExtractionMapper mapper;
    private final CompanyMapper companies;
    private final MaterialPdfReader reader;
    private final MaterialAiClient ai;
    private final MaterialCandidateValidator validator;
    private final MaterialFactPipeline factPipeline = new MaterialFactPipeline();
    private final ObjectMapper json;
    private final TransactionTemplate transaction;
    private final Path root;
    private boolean initialized;
    private final Set<String> activeCompanies = ConcurrentHashMap.newKeySet();
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(8), new ThreadPoolExecutor.AbortPolicy());
    public MaterialExtractionService(MaterialExtractionMapper mapper, CompanyMapper companies,
            MaterialPdfReader reader, MaterialAiClient ai, MaterialCandidateValidator validator, ObjectMapper json,
            org.springframework.transaction.PlatformTransactionManager manager,
            @Value("${materials.storage-dir:${MATERIALS_STORAGE_DIR:}}") String storage) {
        this.mapper=mapper; this.companies=companies; this.reader=reader; this.ai=ai;
        this.validator=validator; this.json=json; this.transaction=new TransactionTemplate(manager);
        Path base=Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if (storage.isBlank()) {
            for (Path p=base; p!=null; p=p.getParent()) {
                if (Files.isDirectory(p.resolve("frontend")) && Files.isDirectory(p.resolve("backend"))) { base=p; break; }
            }
            root=base.resolve(".runtime/materials");
        } else root=Path.of(storage).toAbsolutePath();
    }
    private synchronized void initialize() {
        if (!initialized) { mapper.createTasks(); mapper.createAudit(); mapper.recover(); initialized=true; }
    }
    public Company company(String code) {
        Company company=companies.selectById(code);
        if(company==null) throw new BusinessException(401,"企业身份已失效，请重新登录");
        return company;
    }
    public Map<String,Object> template(String code) {
        Company c=company(code);
        return Map.of("companyCode",c.getCompanyCode(),"industryKey",c.getIndustryKey(),"industryName",c.getIndustryName(),"version",MaterialIndicatorCatalog.VERSION,
                "aiConfigured",ai.configured(),"indicators",MaterialIndicatorCatalog.forIndustry(c.getIndustryKey()));
    }
    public List<MaterialExtractionVO> list(String code) {
        initialize(); return mapper.list(code).stream().map(this::view).toList();
    }
    public MaterialExtractionVO get(String code,String id) { return view(require(code,id)); }
    private Map<String,Object> require(String code,String id) {
        initialize(); var row=mapper.get(code,id);
        if(row==null || row.isEmpty()) throw new BusinessException("材料任务不存在或无权访问");
        return row;
    }
    private MaterialExtractionVO view(Map<String,Object> row) {
        Result result=null;
        try { if(row.get("resultJson")!=null) result=json.readValue(row.get("resultJson").toString(),Result.class); }
        catch(Exception e) { throw new BusinessException("任务结果读取失败"); }
        return new MaterialExtractionVO(row.get("id").toString(),row.get("fileName").toString(),row.get("industryKey").toString(),
                ((Number)row.get("dataYear")).intValue(),row.get("status").toString(),((Number)row.get("progress")).intValue(),
                row.get("message").toString(),result,String.valueOf(row.get("createdAt")));
    }
    public String upload(String code,int year,boolean consent,MultipartFile file) {
        initialize(); Company c=company(code); MaterialIndicatorCatalog.forIndustry(c.getIndustryKey());
        if(!consent) throw new BusinessException("请先确认将相关材料片段发送到已配置的AI服务");
        if(year<1990 || year>java.time.Year.now().getValue()+1) throw new BusinessException("数据年份不合理");
        if(!ai.configured()) throw new BusinessException("请先启用并配置豆包AI服务");
        if(file.isEmpty() || file.getSize()>100L*1024*1024) throw new BusinessException("请选择100MB以内的PDF");
        String name=Objects.toString(file.getOriginalFilename(),"material.pdf").replace('\\','/');
        name=name.substring(name.lastIndexOf('/')+1);
        if(!name.toLowerCase(Locale.ROOT).endsWith(".pdf") || name.length()>255) throw new BusinessException("文件必须为PDF且名称不超过255字符");
        if(!activeCompanies.add(code)) throw new BusinessException("本企业已有提取任务，请等待完成");
        String id=UUID.randomUUID().toString();
        boolean inserted=false;
        try {
            // 大文件只读取签名，随后流式落盘，不把整个100MB文件装入Java堆。
            try(var input=file.getInputStream()) {
                if(!new String(input.readNBytes(5),java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))
                    throw new BusinessException("文件内容不是有效PDF");
            }
            Files.createDirectories(root);
            try(var input=file.getInputStream()) { Files.copy(input,root.resolve(id+".pdf")); }
            mapper.insert(id,code,name,c.getIndustryKey(),year); inserted=true;
            workers.execute(()->process(id,c,year));
            return id;
        } catch(Exception e) {
            activeCompanies.remove(code);
            if(inserted) mapper.update(id,"FAILED",0,"任务未能启动，请稍后重试",null);
            if(e instanceof BusinessException business) throw business;
            throw new BusinessException("上传失败或任务队列已满，请稍后重试");
        }
    }
    private void process(String id,Company company,int year) {
        try {
            mapper.update(id,"PARSING",15,"正在解析PDF；扫描页执行本机OCR，多页材料可能需要数分钟",null);
            List<Page> pages=reader.read(root.resolve(id+".pdf"));
            var fields=MaterialIndicatorCatalog.forIndustry(company.getIndustryKey());
            List<Integer> ocr=pages.stream().filter(p->p.text().replaceAll("\\s+","").length()<20).map(Page::number).toList();
            // 逐页检索，每个指标优先取最高相关的四个页面，共用页面去重。
            // 只发送选中的片段，结果保留searchedPages，避免将局部检索误称全文覆盖。
            LinkedHashSet<Integer> selected=new LinkedHashSet<>();
            for(var field:fields) pages.stream().filter(p->score(p,field.keywords())>0)
                    .sorted(Comparator.<Page>comparingInt(p->score(p,field.keywords())).reversed())
                    .limit(4).forEach(p->selected.add(p.number()));
            pages.stream().filter(p->score(p,List.of("指标","绩效","研发","能耗","排放","产量","收入","利用率"))>0)
                    .sorted(Comparator.<Page>comparingInt(p->score(p,List.of("指标","绩效","研发","能耗","排放","产量","收入","利用率"))).reversed())
                    .limit(12).forEach(p->selected.add(p.number()));
            if(selected.isEmpty()) throw new BusinessException(ocr.size()==pages.size()
                    ? "未检测到可用文字层，请先OCR或上传文字型PDF" : "未检索到本行业模板相关指标，请核对材料或补充报告");
            List<Page> snippets=new ArrayList<>(); int remaining=96000;
            // 总字数受限时优先保留含实际数字与指标的页面，而不是按页码截断到报告前言。
            var ranked=pages.stream().filter(p->selected.contains(p.number())).sorted(Comparator.<Page>comparingInt(
                    p->fields.stream().mapToInt(f->score(p,f.keywords())).sum()
                            +score(p,List.of("指标","绩效","研发","能耗","排放","产量","收入","利用率"))).reversed()).limit(24).toList();
            for(Page page:ranked) if(remaining>0) {
                String text=page.text().substring(0,Math.min(page.text().length(),Math.min(6000,remaining)));
                snippets.add(new Page(page.number(),text)); remaining-=text.length();
            }
            mapper.update(id,"EXTRACTING",45,"正在调用AI提取候选值与证据，随后执行口径校验",null);
            // 事实提取不接收评价模板；分批保存结构化响应，之后才执行本地模板匹配。
            List<com.fasterxml.jackson.databind.JsonNode> responses=new ArrayList<>();
            List<String> failedPages=new ArrayList<>();
            List<Integer> attemptedPages=new ArrayList<>();
            int consecutiveFailures=0;
            for(int offset=0;offset<snippets.size();offset++) {
                var batch=snippets.subList(offset,offset+1);
                attemptedPages.add(batch.get(0).number());
                try { responses.add(ai.extract(factPipeline.prompt(batch))); consecutiveFailures=0; }
                catch(BusinessException e) {
                    // 单页格式失败不丢弃其他页；错误消息经过AI客户端清洗，不含供应商响应或密钥。
                    failedPages.add("第"+batch.get(0).number()+"页："+e.getMessage());
                    consecutiveFailures++;
                }
                var partialFacts=factPipeline.facts(responses);
                var partial=new Result("material-facts-v3",pages.size(),ocr,snippets.subList(0,offset+1).stream().map(Page::number).toList(),
                        factPipeline.match(partialFacts,fields,snippets,year,company.getCompanyName(),validator),partialFacts,List.copyOf(responses),
                        "已处理"+(offset+1)+"/"+snippets.size()+"页，成功"+responses.size()+"页，失败"+failedPages.size()+"页");
                mapper.update(id,"EXTRACTING",45+(offset+batch.size())*40/snippets.size(),partial.diagnostic(),json.writeValueAsString(partial));
                // 连续服务故障时及时结束，避免对失效配置逐页等待至数十分钟。
                if(consecutiveFailures>=3) break;
            }
            if(responses.isEmpty()) throw new BusinessException(failedPages.isEmpty()?"未获得AI结果":failedPages.get(0));
            var facts=factPipeline.facts(responses);
            var candidates=factPipeline.match(facts,fields,snippets,year,company.getCompanyName(),validator);
            String diagnostic="AI成功返回"+responses.size()+"批，提取"+facts.size()+"条原始事实；模板外"
                    +candidates.stream().filter(c->"OUTSIDE".equals(c.status())).count()+"条。只检索所列页面，不代表全文覆盖；扫描页使用本机OCR，须核对原页。"
                    +(failedPages.isEmpty()?"":"部分页面提取失败："+String.join("；",failedPages));
            Result result=new Result("material-facts-v3",pages.size(),ocr,List.copyOf(attemptedPages),candidates,facts,responses,diagnostic);
            long found=candidates.stream().filter(c->!"MISSING".equals(c.status())).count();
            mapper.update(id,"COMPLETED",100,found==0
                    ? "未提取到候选值：AI请求已返回，请检查报告行业、数据年份及送审页面；下表仅为模板待补项"
                    : "已提取"+found+"项候选"+(failedPages.isEmpty()?"":"（部分页面失败，请查看提取诊断）")
                    +"，请核对主体、口径和原文；写入待评估记录为可选步骤",json.writeValueAsString(result));
        } catch(BusinessException e) { fail(id,company.getCompanyCode(),e.getMessage()); }
        catch(Exception e) { fail(id,company.getCompanyCode(),"材料解析或提取失败，请检查PDF并重试"); }
        finally { activeCompanies.remove(company.getCompanyCode()); }
    }
    /** 中途失败保留成功批次，不把部分结果误报为完整成功。 */
    private void fail(String id,String code,String message) {
        var task=mapper.get(code,id);
        mapper.update(id,"FAILED",100,message,task!=null && task.get("resultJson")!=null?task.get("resultJson").toString():null);
    }
    private int score(Page page,List<String> keywords) {
        String text=MaterialCandidateValidator.compact(page.text());
        int matches=keywords.stream().mapToInt(word->text.contains(word)?1:0).sum();
        if(matches==0) return 0;
        // 优先数据密集的绩效表，降低仅提及关键词的董事长致辞、目录和政策段落排名。
        int numbers=(int)java.util.regex.Pattern.compile("\\d+[.,]\\d+|\\d+%").matcher(text).results().limit(20).count();
        return matches*10+numbers;
    }
    public Path file(String code,String id) { require(code,id); return root.resolve(UUID.fromString(id)+".pdf"); }
    public Map<String,Object> targets(String code,String id) {
        var task=get(code,id); var industry=IndustryDataCatalog.requireIndustry(task.industryKey());
        return Map.of("rows",mapper.targets(industry.pendingTableName(),code,task.dataYear()),"adoptedRows",mapper.adoptedRows(id,code));
    }
    /** 锁定目标行、重新验证身份/年份、比较旧值，字段更新与采用记录同事务提交。 */
    public void adopt(String code,String id,MaterialAdoptRequest request) {
        initialize();
        transaction.executeWithoutResult(tx->{
            var task=get(code,id);
            if(!"COMPLETED".equals(task.status()) || task.result()==null) throw new BusinessException("提取尚未完成");
            if(!request.subjectConfirmed()) throw new BusinessException("请确认材料主体、期间与核算范围属于本企业");
            if(mapper.adoptedRows(id,code).contains(request.rowId())) throw new BusinessException("该任务已采用到此记录，不能重复提交");
            var industry=IndustryDataCatalog.requireIndustry(task.industryKey());
            var row=mapper.lockTarget(industry.pendingTableName(),request.rowId(),code,task.dataYear());
            if(row==null) throw new BusinessException("目标记录不属于本企业/年份或已被授信归档，请刷新");
            Map<String,Object> values=new LinkedHashMap<>(); List<String> assignments=new ArrayList<>();
            List<Map<String,Object>> changes=new ArrayList<>(); Set<String> seen=new HashSet<>();
            for(var item:request.items()) {
                if(!seen.add(item.field())) throw new BusinessException("不可重复提交同一指标");
                var candidate=task.result().candidates().stream().filter(c->c.field().equals(item.field()) && Set.of("READY","REVIEW").contains(c.status())).findFirst()
                        .orElseThrow(()->new BusinessException("不在本次提取范围内的指标"));
                if(!candidate.evidenceVerified() || !Set.of("REVIEW","READY").contains(candidate.status()))
                    throw new BusinessException("缺失、口径不符或证据未核验的指标不能采用");
                // 首版只确认已核验候选，不支持用手工改值绕过原文证据校验。
                if(candidate.value()==null || candidate.value().compareTo(item.value())!=0)
                    throw new BusinessException("采用值必须与已核验的候选值一致");
                var field=IndustryDataCatalog.requireField(task.industryKey(),item.field());
                Object old=row.get(field.column());
                BigDecimal previous=old==null?null:new BigDecimal(old.toString());
                if(previous==null?item.expectedOldValue()!=null:item.expectedOldValue()==null || previous.compareTo(item.expectedOldValue())!=0)
                    throw new BusinessException("目标值已发生变化，请刷新后重新确认");
                values.put(field.prop(),item.value()); assignments.add("`"+field.column()+"`=#{values."+field.prop()+"}");
                Map<String,Object> change=new LinkedHashMap<>(); change.put("field",field.prop()); change.put("label",field.label());
                change.put("oldValue",previous); change.put("newValue",item.value()); change.put("page",candidate.page());
                change.put("quote",candidate.quote()); change.put("reviewNote",item.reviewNote()); changes.add(change);
            }
            try {
                mapper.audit(id,code,industry.pendingTableName(),request.rowId(),json.writeValueAsString(changes));
                if(mapper.apply(industry.pendingTableName(),request.rowId(),String.join(",",assignments),values)!=1)
                    throw new BusinessException("记录更新失败");
            } catch(com.fasterxml.jackson.core.JsonProcessingException e) { throw new BusinessException("采用记录保存失败"); }
        });
    }
    @PreDestroy public void stop() { workers.shutdownNow(); }
}
