package com.transfinance.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/** 可选真实扫描件单页验收；只读取指定原件，临时裁页交给本机OCR，不调用在线AI。 */
class MaterialOcrTest {
    @TempDir Path temporary;
    @Test void recognizesRealScannedPage() throws Exception {
        String input=System.getProperty("material.ocr.pdf", "");
        assumeTrue(!input.isBlank());
        int page=Integer.getInteger("material.ocr.page", 20);
        Path sample=temporary.resolve("sample.pdf");
        try(var source=Loader.loadPDF(Path.of(input).toFile()); var target=new PDDocument()) {
            var imported=target.importPage(source.getPage(page-1));
            imported.setResources(source.getPage(page-1).getResources());
            target.save(sample.toFile());
        }
        var pages=new MaterialPdfReader().read(sample);
        System.out.println("OCR verified: characters="+pages.get(0).text().length());
        assertTrue(pages.get(0).text().replaceAll("\\s+", "").length()>50);
        assertTrue(java.util.regex.Pattern.compile("\\d").matcher(pages.get(0).text()).find());
    }
}
