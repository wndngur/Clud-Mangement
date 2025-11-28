package com.example.clubmanagement.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.example.clubmanagement.models.DocumentData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

public class PdfGenerator {
    private static final String TAG = "PdfGenerator";

    public interface PdfGenerationCallback {
        void onSuccess(File pdfFile);
        void onFailure(Exception e);
    }

    /**
     * 문서 템플릿 타입
     */
    public enum DocumentTemplate {
        ACTIVITY_REPORT("활동 보고서"),
        MEETING_MINUTES("회의록"),
        MEMBER_APPLICATION("가입 신청서"),
        GENERAL("일반 문서");

        private final String koreanName;

        DocumentTemplate(String koreanName) {
            this.koreanName = koreanName;
        }

        public String getKoreanName() {
            return koreanName;
        }
    }

    /**
     * 문서 데이터를 PDF로 생성
     */
    public static void generatePdfWithSignature(
            DocumentData documentData,
            String signatureUrl,
            File outputFile,
            PdfGenerationCallback callback
    ) {
        new Thread(() -> {
            try {
                // PDF Writer 생성
                PdfWriter writer = new PdfWriter(new FileOutputStream(outputFile));
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc);

                // 제목 추가
                Paragraph title = new Paragraph(documentData.getTitle())
                        .setFontSize(20)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER);
                document.add(title);

                // 문서 타입 추가
                Paragraph type = new Paragraph("[" + documentData.getType() + "]")
                        .setFontSize(12)
                        .setTextAlignment(TextAlignment.CENTER);
                document.add(type);

                document.add(new Paragraph("\n"));

                // 문서 내용 추가
                Paragraph content = new Paragraph(documentData.getContent())
                        .setFontSize(12);
                document.add(content);

                // 서명 필요 시 서명 이미지 추가
                if (documentData.isRequiresSignature() && signatureUrl != null && !signatureUrl.isEmpty()) {
                    document.add(new Paragraph("\n\n"));

                    Paragraph signatureLabel = new Paragraph("서명:")
                            .setFontSize(12);
                    document.add(signatureLabel);

                    document.add(new Paragraph("\n"));

                    // 서명 이미지 다운로드 및 추가
                    try {
                        byte[] imageBytes = downloadImage(signatureUrl);
                        if (imageBytes != null) {
                            Image signatureImage = new Image(ImageDataFactory.create(imageBytes));

                            // 서명 위치 설정
                            DocumentData.SignaturePosition pos = documentData.getSignaturePosition();
                            if (pos != null && pos.getWidth() > 0) {
                                signatureImage.setWidth(pos.getWidth());
                            } else {
                                signatureImage.setWidth(150);
                            }

                            document.add(signatureImage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "서명 이미지 추가 실패: " + e.getMessage());
                    }

                    // 서명선
                    document.add(new Paragraph("_".repeat(30))
                            .setFontSize(10));
                }

                // 문서 정보 footer
                document.add(new Paragraph("\n\n"));
                String footer = "생성일: " + (documentData.getCreatedAt() != null ?
                        documentData.getCreatedAt().toDate().toString() : "N/A");
                document.add(new Paragraph(footer)
                        .setFontSize(8)
                        .setTextAlignment(TextAlignment.RIGHT));

                document.close();

                Log.d(TAG, "PDF 생성 완료: " + outputFile.getAbsolutePath());
                callback.onSuccess(outputFile);

            } catch (Exception e) {
                Log.e(TAG, "PDF 생성 실패", e);
                callback.onFailure(e);
            }
        }).start();
    }

    /**
     * 활동 보고서 템플릿
     */
    public static String getActivityReportTemplate(String activityName, String date, int memberCount, String description) {
        return String.format(
                "활동명: %s\n\n" +
                "날짜: %s\n" +
                "참여 인원: %d명\n\n" +
                "활동 내용:\n%s\n\n" +
                "아래의 서명은 제출자의 의사 확인을 위해 사용됩니다.",
                activityName, date, memberCount, description
        );
    }

    /**
     * 회의록 템플릿
     */
    public static String getMeetingMinutesTemplate(String meetingTitle, String date, String attendees, String agenda, String decisions) {
        return String.format(
                "회의명: %s\n\n" +
                "날짜: %s\n" +
                "참석자: %s\n\n" +
                "안건:\n%s\n\n" +
                "결정 사항:\n%s\n\n" +
                "아래 서명란은 참석자 확인용입니다.",
                meetingTitle, date, attendees, agenda, decisions
        );
    }

    /**
     * 가입 신청서 템플릿
     */
    public static String getMemberApplicationTemplate(String name, String studentId, String department, String email, String phone, String reason) {
        return String.format(
                "지원자 정보\n\n" +
                "이름: %s\n" +
                "학번: %s\n" +
                "학과: %s\n" +
                "이메일: %s\n" +
                "연락처: %s\n\n" +
                "가입 동기:\n%s\n\n" +
                "위 내용이 사실임을 확인하며, 본인의 서명으로 이를 증명합니다.",
                name, studentId, department, email, phone, reason
        );
    }

    /**
     * URL에서 이미지 다운로드
     */
    private static byte[] downloadImage(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            InputStream inputStream = url.openStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "이미지 다운로드 실패: " + e.getMessage());
            return null;
        }
    }
}
