package com.transfinance.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import com.transfinance.vo.MaterialExtractionVO.Page;
import com.transfinance.exception.BusinessException;
import java.nio.file.Path;
import java.util.*;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;

/** 保留物理PDF页码，不把印刷页码当作文件页码；无文字层页面交给OCR。 */
@Component
public class MaterialPdfReader {
    public List<Page> read(Path file) throws java.io.IOException {
        try (var pdf = Loader.loadPDF(file.toFile())) {
            if (pdf.isEncrypted()) throw new BusinessException("暂不支持加密PDF，请上传未加密版本");
            if (pdf.getNumberOfPages() > 500) throw new BusinessException("单份材料最多500页，请拆分后上传");
            var stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            List<Page> pages = new ArrayList<>();
            int length = 0;
            Path script = null;
            try {
            for (int i = 1; i <= pdf.getNumberOfPages(); i++) {
                stripper.setStartPage(i); stripper.setEndPage(i);
                String text = stripper.getText(pdf).replace("\u0000", "").trim();
                // 文字页不调用OCR；扫描页在本机识别，不要求AI模型支持图片输入。
                if (text.replaceAll("\\s+", "").length() < 20
                        && System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows")) {
                    if (script == null) {
                        script = Files.createTempFile("material-ocr-", ".ps1");
                        try (var source = getClass().getResourceAsStream("/material-ocr.ps1")) {
                            if (source == null) throw new BusinessException("OCR组件未打包，请重新构建后端");
                            Files.copy(source, script, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                    String recognized = recognize(new PDFRenderer(pdf), i - 1, pdf.getPage(i - 1).getCropBox(), script);
                    if (!recognized.isBlank()) text = recognized;
                }
                length += text.length();
                if (length > 1_000_000) throw new BusinessException("材料文字量过大，请拆分后上传");
                pages.add(new Page(i, text));
            }
            return pages;
            } finally { if (script != null) Files.deleteIfExists(script); }
        }
    }

    /** 限制渲染尺寸和单页等待时长；临时文件逐个清理，不保留材料副本。 */
    private String recognize(PDFRenderer renderer, int index, org.apache.pdfbox.pdmodel.common.PDRectangle box, Path script)
            throws java.io.IOException {
        Path image = Files.createTempFile("material-page-", ".png");
        Path output = Files.createTempFile("material-text-", ".txt");
        Process process = null;
        try {
            float scale = Math.min(2f, 2400f / Math.max(box.getWidth(), box.getHeight()));
            var bitmap = renderer.renderImage(index, scale, ImageType.RGB);
            try { javax.imageio.ImageIO.write(bitmap, "png", image.toFile()); }
            finally { bitmap.flush(); }
            process = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                    "-File", script.toString(), "-ImagePath", image.toString(), "-OutputPath", output.toString())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            if (!process.waitFor(60, TimeUnit.SECONDS)) throw new BusinessException("PDF第" + (index + 1) + "页OCR超时，请拆分材料重试");
            if (process.exitValue() != 0) throw new BusinessException("本机中文OCR不可用，请在Windows语言设置安装简体中文OCR，或上传含文字层的PDF");
            return Files.readString(output).trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new BusinessException("OCR任务已中断，请重新上传");
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            Files.deleteIfExists(image); Files.deleteIfExists(output);
        }
    }
}
