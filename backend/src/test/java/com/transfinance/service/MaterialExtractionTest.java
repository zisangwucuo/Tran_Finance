package com.transfinance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transfinance.controller.MaterialExtractionController;
import com.transfinance.exception.BusinessException;
import com.transfinance.support.MaterialIndicatorCatalog;
import com.transfinance.vo.MaterialExtractionVO.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 重点验证证据、年份、口径与企业会话边界，不依赖在线AI或修改真实数据。 */
class MaterialExtractionTest {
    ObjectMapper json = new ObjectMapper();
    List<MaterialIndicatorCatalog.Indicator> template = MaterialIndicatorCatalog.forIndustry("steel").stream()
            .filter(f -> f.field().equals("steelCarbonEmissionIntensity")).toList();
    String quote = "2024年，宝钢股份四基地的碳排放强度为2.00吨二氧化碳/吨粗钢。";
    Map<String,Object> candidate() {
        return new HashMap<>(Map.of("field","steelCarbonEmissionIntensity","rawValue","2.00",
                "unit","吨二氧化碳/吨粗钢","year",2024,"denominator","粗钢产量","boundary","四基地",
                "reportCompany","宝钢股份","page",40,"quote",quote));
    }
    Candidate validate(Map<String,Object> candidate) {
        return new MaterialCandidateValidator().validate(json.valueToTree(Map.of("candidates",List.of(candidate))),
                template,List.of(new Page(40,quote)),2024,"宝钢股份").get(0);
    }
    @Test void validEvidenceStillRequiresBoundaryReview() {
        var row=validate(candidate()); assertTrue(row.evidenceVerified()); assertEquals("REVIEW",row.status());
        assertEquals("2.00",row.value().toPlainString());
    }
    @Test void fabricatedEvidenceIsBlocked() {
        var row=candidate(); row.put("quote","2024年，其他企业碳排放强度为2.00，与材料没有关联");
        assertEquals("BLOCKED",validate(row).status()); assertFalse(validate(row).evidenceVerified());
    }
    @Test void wrongYearAndFinishedSteelAreBlocked() {
        var row=candidate(); row.put("year",2020); assertEquals("BLOCKED",validate(row).status());
        row=candidate(); row.put("denominator","成品钢材产量"); assertEquals("BLOCKED",validate(row).status());
    }
    @Test void missingIsNotZeroAndUnknownFieldsAreIgnored() {
        var rows=new MaterialCandidateValidator().validate(json.valueToTree(Map.of("candidates",List.of(Map.of("field","unknown","rawValue","0")))),
                template,List.of(new Page(40,quote)),2024,"宝钢股份");
        assertEquals("MISSING",rows.get(0).status()); assertNull(rows.get(0).value());
    }
    @Test void substringsAreNotNumericEvidence() {
        quote=quote.replace("2.00","12.00"); assertEquals("BLOCKED",validate(candidate()).status());
    }
    @Test void duplicateValuesRequireNewExtraction() {
        var rows=new MaterialCandidateValidator().validate(json.valueToTree(Map.of("candidates",List.of(candidate(),candidate()))),
                template,List.of(new Page(40,quote)),2024,"宝钢股份");
        assertEquals("BLOCKED",rows.get(0).status());
    }
    @Test void companyCannotSpoofOwnerUsingRequestParameter() {
        var service=mock(MaterialExtractionService.class);
        var controller=new MaterialExtractionController(service);
        var request=new MockHttpServletRequest(); request.setParameter("companyCode","other");
        assertThrows(BusinessException.class,()->controller.get("task",request)); verifyNoInteractions(service);
        request.getSession().setAttribute("companyCode","own"); controller.get("task",request);
        verify(service).get("own","task");
    }
    @Test void pdfReaderPreservesPhysicalPagesAndBlankPages(@TempDir Path dir) throws Exception {
        Path file=dir.resolve("sample.pdf");
        try(var pdf=new PDDocument()) {
            pdf.addPage(new PDPage()); var page=new PDPage(); pdf.addPage(page);
            try(var stream=new PDPageContentStream(pdf,page)) {
                stream.beginText(); stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);
                stream.newLineAtOffset(50,700); stream.showText("Year 2024 carbon intensity 2.00"); stream.endText();
            }
            pdf.save(file.toFile());
        }
        var pages=new MaterialPdfReader().read(file);
        assertEquals(2,pages.size()); assertTrue(pages.get(0).text().isBlank());
        assertEquals(2,pages.get(1).number()); assertTrue(pages.get(1).text().contains("2.00"));
    }
    @Test void providedReportHasTraceableCarbonEvidence() throws Exception {
        String sample=System.getProperty("material.sample");
        Assumptions.assumeTrue(sample!=null,"Optional local report integration test");
        var pages=new MaterialPdfReader().read(Path.of(sample));
        String text=MaterialCandidateValidator.compact(pages.get(39).text());
        assertTrue(text.contains("2.00")); assertTrue(text.contains("吨粗钢"));
        System.out.println("PDF integration verified: physical page 40 contains carbon value and crude-steel denominator");
    }
}
