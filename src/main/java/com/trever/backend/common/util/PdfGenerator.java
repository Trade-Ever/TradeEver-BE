package com.trever.backend.common.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class PdfGenerator {

    /**
     * HTML 문자열을 PDF 파일로 변환
     * @param htmlContent 변환할 HTML 문자열
     * @param outputPath 저장할 PDF 파일 경로
     */
    public static void generatePdfFromHtml(String htmlContent, String outputPath) {
        try (OutputStream os = new FileOutputStream(outputPath)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            // html 적용
            builder.withHtmlContent(htmlContent, null);

            // 나눔명조 Regular
            String regularPath = "src/main/resources/fonts/NanumMyeongjo-Regular.ttf";
            builder.useFont(new File(regularPath), "NanumMyeongjo", 400,
                    PdfRendererBuilder.FontStyle.NORMAL, true);

            // 나눔명조 Bold
            String boldPath = "src/main/resources/fonts/NanumMyeongjo-Bold.ttf";
            builder.useFont(new File(boldPath), "NanumMyeongjo", 700,
                    PdfRendererBuilder.FontStyle.NORMAL, true);

            builder.toStream(os);
            builder.run();
        } catch (Exception e) {
            throw new RuntimeException("PDF 변환 실패", e);
        }
    }
}
