package com.trever.backend.common.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.FileOutputStream;
import java.io.OutputStream;

import org.springframework.core.io.ClassPathResource;
import java.io.InputStream;

public class PdfGenerator {

    public static void generatePdfFromHtml(String htmlContent, String outputPath) {
        try (OutputStream os = new FileOutputStream(outputPath)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, null);

            // Regular 폰트
            try (InputStream regularFont = new ClassPathResource("fonts/NanumMyeongjo-Regular.ttf").getInputStream()) {
                builder.useFont(() -> regularFont, "NanumMyeongjo", 400,
                        PdfRendererBuilder.FontStyle.NORMAL, true);
            }

            // Bold 폰트
            try (InputStream boldFont = new ClassPathResource("fonts/NanumMyeongjo-Bold.ttf").getInputStream()) {
                builder.useFont(() -> boldFont, "NanumMyeongjo", 700,
                        PdfRendererBuilder.FontStyle.NORMAL, true);
            }

            builder.toStream(os);
            builder.run();
        } catch (Exception e) {
            throw new RuntimeException("PDF 변환 실패", e);
        }
    }
}

