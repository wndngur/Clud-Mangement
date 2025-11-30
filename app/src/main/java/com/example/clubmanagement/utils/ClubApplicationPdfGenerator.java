package com.example.clubmanagement.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.example.clubmanagement.models.Club;
import com.example.clubmanagement.models.Member;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 중앙동아리 신청서 PDF 생성기
 * 23페이지 형식의 신청서를 자동으로 생성합니다.
 */
public class ClubApplicationPdfGenerator {
    private static final String TAG = "ClubPdfGenerator";

    private Context context;
    private Club club;
    private List<Member> members;
    private String professorName;
    private String presidentName;
    private String vicePresidentName;
    private String secretaryName;
    private String treasurerName;
    private String academicYear; // 학년도
    private String applicationType; // "신규" or "갱신"

    // 서명 이미지 (Base64 또는 Bitmap)
    private Map<String, Bitmap> memberSignatures; // memberId -> signature bitmap
    private Bitmap professorSignature;
    private Bitmap presidentSignature;

    // 회칙 파일 URI
    private Uri clubRulesUri;

    // 활동 결과 데이터
    private List<ActivityResult> activityResults;
    private List<EventPlan> eventPlans;
    private List<BudgetItem> budgetItems;

    // 월별 활동 계획 및 예산
    private Map<String, String> monthlyPlans;
    private Map<String, Long> monthlyBudgets;

    private PdfFont koreanFont;
    private PdfFont koreanBoldFont;

    public ClubApplicationPdfGenerator(Context context) {
        this.context = context;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    public void setMembers(List<Member> members) {
        this.members = members;
    }

    public void setProfessorName(String professorName) {
        this.professorName = professorName;
    }

    public void setOfficers(String president, String vicePresident, String secretary, String treasurer) {
        this.presidentName = president;
        this.vicePresidentName = vicePresident;
        this.secretaryName = secretary;
        this.treasurerName = treasurer;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public void setMemberSignatures(Map<String, Bitmap> signatures) {
        this.memberSignatures = signatures;
    }

    public void setProfessorSignature(Bitmap signature) {
        this.professorSignature = signature;
    }

    public void setPresidentSignature(Bitmap signature) {
        this.presidentSignature = signature;
    }

    public void setClubRulesUri(Uri uri) {
        this.clubRulesUri = uri;
    }

    public void setActivityResults(List<ActivityResult> results) {
        this.activityResults = results;
    }

    public void setEventPlans(List<EventPlan> plans) {
        this.eventPlans = plans;
    }

    public void setBudgetItems(List<BudgetItem> items) {
        this.budgetItems = items;
    }

    public void setMonthlyPlans(Map<String, String> monthlyPlans) {
        this.monthlyPlans = monthlyPlans;
    }

    public void setMonthlyBudgets(Map<String, Long> monthlyBudgets) {
        this.monthlyBudgets = monthlyBudgets;
    }

    /**
     * 한글 폰트 로드 - 여러 시스템 폰트 경로 시도
     */
    private PdfFont loadKoreanFont() throws Exception {
        // 시도할 폰트 경로들 (Android 버전별로 다름)
        // TTC 파일의 경우 한국어 폰트는 보통 인덱스 1에 있음
        String[] fontPaths = {
                // Android 7.0+ (Noto Sans CJK) - 한국어는 인덱스 1
                "/system/fonts/NotoSansCJK-Regular.ttc,1",
                "/system/fonts/NotoSansCJK-Regular.ttc,0",
                "/system/fonts/NotoSansCJKkr-Regular.otf",
                "/system/fonts/NotoSansKR-Regular.otf",
                // 삼성 기기
                "/system/fonts/SamsungOneKorean-Regular.ttf",
                "/system/fonts/SECRobotoLight-Regular.ttf",
                "/system/fonts/SamsungNeoGothic-Regular.ttf",
                // LG 기기
                "/system/fonts/LGSmartGothic.ttf",
                "/system/fonts/LGSmartGothicR.ttf",
                // Android 5.0~6.0
                "/system/fonts/NanumGothic.ttf",
                // Android 4.x
                "/system/fonts/DroidSansFallback.ttf",
                "/system/fonts/DroidSansFallbackFull.ttf"
        };

        // 각 폰트 경로 시도
        for (String fontPath : fontPaths) {
            try {
                String actualPath = fontPath.split(",")[0]; // TTC 파일의 경우 콤마 이전 부분만 체크
                File fontFile = new File(actualPath);
                if (fontFile.exists()) {
                    Log.d(TAG, "폰트 로드 시도: " + fontPath);
                    // 폰트를 임베드하도록 설정
                    PdfFont font = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H,
                            com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
                    Log.d(TAG, "폰트 로드 성공: " + fontPath);
                    return font;
                }
            } catch (Exception e) {
                Log.w(TAG, "폰트 로드 실패: " + fontPath + " - " + e.getMessage());
            }
        }

        // 시스템 폰트 디렉토리 스캔하여 CJK 관련 폰트 찾기
        PdfFont scannedFont = scanForKoreanFont();
        if (scannedFont != null) {
            return scannedFont;
        }

        // 모든 시스템 폰트 실패 시 assets에서 로드 시도
        try {
            InputStream fontStream = context.getAssets().open("fonts/NanumGothic.ttf");
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = fontStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            fontStream.close();
            byte[] fontBytes = buffer.toByteArray();
            Log.d(TAG, "Assets 폰트 로드 성공");
            return PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H,
                    com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
        } catch (Exception e) {
            Log.w(TAG, "Assets 폰트 로드 실패: " + e.getMessage());
        }

        // 최종 대체: 내장 폰트 사용 (한글 깨질 수 있음)
        Log.e(TAG, "한글 폰트를 찾을 수 없습니다! PDF에 한글이 표시되지 않을 수 있습니다.");
        throw new Exception("한글 폰트를 찾을 수 없습니다. 기기에 한글 폰트가 설치되어 있는지 확인해주세요.\n" +
                "앱의 assets/fonts/ 폴더에 NanumGothic.ttf 파일을 추가해주세요.");
    }

    /**
     * 시스템 폰트 디렉토리를 스캔하여 한글 폰트 찾기
     */
    private PdfFont scanForKoreanFont() {
        File fontsDir = new File("/system/fonts");
        if (!fontsDir.exists() || !fontsDir.isDirectory()) {
            Log.w(TAG, "폰트 디렉토리를 찾을 수 없음: /system/fonts");
            return null;
        }

        File[] fontFiles = fontsDir.listFiles();
        if (fontFiles == null) {
            Log.w(TAG, "폰트 파일 목록을 가져올 수 없음");
            return null;
        }

        // 사용 가능한 폰트 목록 로그 출력
        Log.d(TAG, "시스템 폰트 스캔 시작. 총 " + fontFiles.length + "개 파일");

        // 한글/CJK 관련 키워드를 포함하는 폰트 찾기 (우선순위 순)
        String[] koreanKeywords = {"Korean", "KR", "CJK", "Gothic", "Nanum", "Malgun", "Gulim", "Batang", "Dotum", "Samsung", "SEC"};

        for (String keyword : koreanKeywords) {
            for (File fontFile : fontFiles) {
                String fileName = fontFile.getName();
                String fileNameLower = fileName.toLowerCase();

                // .ttf, .ttc, .otf 파일만 처리
                if (!fileNameLower.endsWith(".ttf") && !fileNameLower.endsWith(".ttc") && !fileNameLower.endsWith(".otf")) {
                    continue;
                }

                // 키워드 매칭
                if (fileNameLower.contains(keyword.toLowerCase())) {
                    // TTC 파일은 여러 인덱스 시도
                    if (fileNameLower.endsWith(".ttc")) {
                        for (int ttcIndex = 0; ttcIndex < 5; ttcIndex++) {
                            try {
                                String fontPath = fontFile.getAbsolutePath() + "," + ttcIndex;
                                Log.d(TAG, "TTC 폰트 로드 시도: " + fontPath);
                                PdfFont font = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H,
                                        com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
                                Log.d(TAG, "TTC 폰트 로드 성공: " + fontPath);
                                return font;
                            } catch (Exception e) {
                                // 다음 인덱스 시도
                            }
                        }
                    } else {
                        try {
                            String fontPath = fontFile.getAbsolutePath();
                            Log.d(TAG, "폰트 로드 시도: " + fontPath);
                            PdfFont font = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H,
                                    com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
                            Log.d(TAG, "폰트 로드 성공: " + fontPath);
                            return font;
                        } catch (Exception e) {
                            Log.w(TAG, "폰트 로드 실패: " + fileName + " - " + e.getMessage());
                        }
                    }
                }
            }
        }

        Log.w(TAG, "한글 관련 폰트를 찾지 못함");
        return null;
    }

    /**
     * PDF 생성 및 저장
     * @return 생성된 PDF 파일 경로
     */
    public String generatePdf() throws Exception {
        // 파일 경로 설정
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String fileName = academicYear + "학년도_" + club.getName() + "_중앙동아리_신청서.pdf";
        File outputFile = new File(downloadDir, fileName);

        // PDF 문서 생성
        PdfWriter writer = new PdfWriter(new FileOutputStream(outputFile));
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(50, 50, 50, 50);

        // 한글 폰트 로드 (시스템 폰트 사용)
        koreanFont = loadKoreanFont();
        koreanBoldFont = koreanFont; // Bold 폰트도 같은 폰트 사용

        // 페이지 1: 표지
        addCoverPage(document);

        // 페이지 2: 중앙동아리 승인신청서
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        addApprovalApplicationPage(document);

        // 페이지 3: 지도교수 승낙서
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        addProfessorConsentPage(document);

        // 페이지 4: 회원 명부
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        addMemberListPage(document);

        // 페이지 5: 기구 조직표
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        addOrganizationChartPage(document);

        // 페이지 6~: 운영 회칙 (외부 파일이 있으면 삽입)
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        addClubRulesPages(document, pdfDoc);

        // 행사 결과 및 결산서
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        addEventResultsPage(document);

        // 활동 결과보고서
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        addActivityReportPages(document);

        // 행사계획 및 예산서
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        addEventPlanPage(document);

        // 월 사업/예산 세부계획서
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        addMonthlyPlanPage(document);

        // 동아리실 사용 서약서
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        addClubRoomAgreementPage(document);

        document.close();

        return outputFile.getAbsolutePath();
    }

    /**
     * 페이지 1: 표지
     */
    private void addCoverPage(Document document) {
        // 상단 여백
        document.add(new Paragraph("\n\n\n\n\n"));

        // 학년도
        Paragraph yearPara = new Paragraph(academicYear + "학년도")
                .setFont(koreanBoldFont)
                .setFontSize(28)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(yearPara);

        document.add(new Paragraph("\n\n"));

        // 신청서 유형
        String typeText = "갱신".equals(applicationType) ? "갱신 중앙동아리 신청서" : "신규 중앙동아리 신청서";
        Paragraph typePara = new Paragraph(typeText)
                .setFont(koreanBoldFont)
                .setFontSize(36)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(typePara);

        document.add(new Paragraph("\n\n\n\n"));

        // 동아리명 박스
        Table nameTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(80))
                .setHorizontalAlignment(HorizontalAlignment.CENTER);

        nameTable.addCell(createCell("동아리명", koreanBoldFont, 16, TextAlignment.CENTER, true));
        nameTable.addCell(createCell(club.getName(), koreanBoldFont, 20, TextAlignment.CENTER, true));

        document.add(nameTable);

        document.add(new Paragraph("\n\n\n\n\n\n\n\n"));

        // 학교명 및 날짜
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        String dateStr = sdf.format(new Date());

        Paragraph datePara = new Paragraph(dateStr)
                .setFont(koreanFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(datePara);

        document.add(new Paragraph("\n"));

        Paragraph schoolPara = new Paragraph("나사렛대학교 총학생회")
                .setFont(koreanBoldFont)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(schoolPara);
    }

    /**
     * 페이지 2: 중앙동아리 승인신청서
     */
    private void addApprovalApplicationPage(Document document) {
        // 제목
        Paragraph title = new Paragraph("중앙동아리 승인신청서")
                .setFont(koreanBoldFont)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30);
        document.add(title);

        // 기본 정보 테이블
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                .setWidth(UnitValue.createPercentValue(100));

        // 동아리명
        infoTable.addCell(createHeaderCell("동아리명"));
        infoTable.addCell(createContentCellWithColspan(club.getName(), 3));

        // 설립 연도
        String foundedYear = "";
        if (club.getFoundedAt() != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(club.getFoundedAt().toDate());
            foundedYear = String.valueOf(cal.get(Calendar.YEAR));
        }
        infoTable.addCell(createHeaderCell("설립 연도"));
        infoTable.addCell(createContentCell(foundedYear + "년"));
        infoTable.addCell(createHeaderCell("회원 수"));
        infoTable.addCell(createContentCell(club.getMemberCount() + "명"));

        // 지도교수
        infoTable.addCell(createHeaderCell("지도교수"));
        infoTable.addCell(createContentCellWithColspan(professorName != null ? professorName : club.getProfessor(), 3));

        // 동아리 대표
        infoTable.addCell(createHeaderCell("동아리 대표"));
        infoTable.addCell(createContentCellWithColspan(presidentName != null ? presidentName : "", 3));

        // 동아리방 위치
        infoTable.addCell(createHeaderCell("동아리방 위치"));
        infoTable.addCell(createContentCellWithColspan(club.getLocation() != null ? club.getLocation() : "", 3));

        // 설립 목적
        infoTable.addCell(createHeaderCell("설립 목적"));
        Cell purposeCell = createContentCellWithColspan(club.getPurpose() != null ? club.getPurpose() : "", 3)
                .setMinHeight(80);
        infoTable.addCell(purposeCell);

        document.add(infoTable);

        document.add(new Paragraph("\n\n"));

        // 신청 문구
        Paragraph applyText = new Paragraph(
                "위와 같이 " + academicYear + "학년도 " +
                ("갱신".equals(applicationType) ? "갱신" : "신규") +
                " 중앙동아리 승인을 신청합니다.")
                .setFont(koreanFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(applyText);

        document.add(new Paragraph("\n"));

        // 날짜
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        Paragraph datePara = new Paragraph(sdf.format(new Date()))
                .setFont(koreanFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(datePara);

        document.add(new Paragraph("\n\n"));

        // 서명 영역 - 서명이 (인) 위에 오도록 배치
        Table signTable = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .setWidth(UnitValue.createPercentValue(80))
                .setHorizontalAlignment(HorizontalAlignment.CENTER);

        signTable.addCell(new Cell().add(new Paragraph("동아리 대표: " + (presidentName != null ? presidentName : ""))
                .setFont(koreanFont).setFontSize(12))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.LEFT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE));

        // (인) 텍스트와 서명 이미지를 겹치도록 배치
        Cell signCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER);

        if (presidentSignature != null) {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                presidentSignature.compress(Bitmap.CompressFormat.PNG, 100, stream);
                ImageData imageData = ImageDataFactory.create(stream.toByteArray());
                Image signImage = new Image(imageData).setWidth(50).setHeight(25);

                // 서명 이미지와 (인) 텍스트를 함께 배치
                Table innerTable = new Table(1).setWidth(UnitValue.createPercentValue(100));
                Cell imgCell = new Cell().add(signImage.setHorizontalAlignment(HorizontalAlignment.CENTER))
                        .setBorder(Border.NO_BORDER)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(-10); // 서명이 (인) 위에 겹치도록
                innerTable.addCell(imgCell);

                Cell inCell = new Cell().add(new Paragraph("(인)").setFont(koreanFont).setFontSize(12))
                        .setBorder(Border.NO_BORDER)
                        .setTextAlignment(TextAlignment.CENTER);
                innerTable.addCell(inCell);

                signCell.add(innerTable);
            } catch (Exception e) {
                Log.e(TAG, "서명 이미지 추가 실패", e);
                signCell.add(new Paragraph("(인)").setFont(koreanFont).setFontSize(12));
            }
        } else {
            signCell.add(new Paragraph("(인)").setFont(koreanFont).setFontSize(12));
        }
        signTable.addCell(signCell);

        document.add(signTable);

        document.add(new Paragraph("\n\n\n"));

        // 수신
        Paragraph receiverPara = new Paragraph("나사렛대학교 총학생회장 귀하")
                .setFont(koreanBoldFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(receiverPara);
    }

    /**
     * 페이지 3: 지도교수 승낙서
     */
    private void addProfessorConsentPage(Document document) {
        Paragraph title = new Paragraph("지도교수 승낙서")
                .setFont(koreanBoldFont)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30);
        document.add(title);

        // 정보 테이블
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100));

        infoTable.addCell(createHeaderCell("동아리명"));
        infoTable.addCell(createContentCell(club.getName()));

        infoTable.addCell(createHeaderCell("지도교수"));
        infoTable.addCell(createContentCell(professorName != null ? professorName : club.getProfessor()));

        infoTable.addCell(createHeaderCell("소속 학과"));
        infoTable.addCell(createContentCell("")); // 학과 정보 추가 필요

        infoTable.addCell(createHeaderCell("연락처"));
        infoTable.addCell(createContentCell("")); // 연락처 추가 필요

        document.add(infoTable);

        document.add(new Paragraph("\n\n\n"));

        // 승낙 문구
        String consentText = "본인은 " + club.getName() + " 동아리의 지도교수로서 " +
                "해당 동아리의 건전한 활동을 지도·감독할 것을 승낙합니다.\n\n" +
                "동아리의 모든 활동에 대해 책임지고 지도하며, " +
                "학교의 교육 목표에 부합하는 활동이 이루어지도록 노력하겠습니다.";

        Paragraph consentPara = new Paragraph(consentText)
                .setFont(koreanFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.LEFT)
                .setFirstLineIndent(20);
        document.add(consentPara);

        document.add(new Paragraph("\n\n\n"));

        // 날짜
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        Paragraph datePara = new Paragraph(sdf.format(new Date()))
                .setFont(koreanFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(datePara);

        document.add(new Paragraph("\n\n"));

        // 서명
        Table signTable = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .setWidth(UnitValue.createPercentValue(70))
                .setHorizontalAlignment(HorizontalAlignment.CENTER);

        signTable.addCell(new Cell()
                .add(new Paragraph("지도교수: " + (professorName != null ? professorName : club.getProfessor()))
                .setFont(koreanFont).setFontSize(14))
                .setBorder(Border.NO_BORDER));

        Cell profSignCell = new Cell().setBorder(Border.NO_BORDER);
        profSignCell.add(new Paragraph("(인)").setFont(koreanFont).setFontSize(12));
        if (professorSignature != null) {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                professorSignature.compress(Bitmap.CompressFormat.PNG, 100, stream);
                ImageData imageData = ImageDataFactory.create(stream.toByteArray());
                Image signImage = new Image(imageData).setWidth(60).setHeight(30);
                profSignCell.add(signImage);
            } catch (Exception e) {
                Log.e(TAG, "교수 서명 이미지 추가 실패", e);
            }
        }
        signTable.addCell(profSignCell);

        document.add(signTable);

        document.add(new Paragraph("\n\n\n\n"));

        Paragraph receiverPara = new Paragraph("나사렛대학교 총학생회장 귀하")
                .setFont(koreanBoldFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(receiverPara);
    }

    /**
     * 페이지 4: 회원 명부
     */
    private void addMemberListPage(Document document) {
        Paragraph title = new Paragraph("회원 명부")
                .setFont(koreanBoldFont)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        Paragraph clubNamePara = new Paragraph("동아리명: " + club.getName())
                .setFont(koreanFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(10);
        document.add(clubNamePara);

        // 회원 테이블
        Table memberTable = new Table(UnitValue.createPercentArray(new float[]{8, 15, 15, 20, 15, 12, 15}))
                .setWidth(UnitValue.createPercentValue(100));

        // 헤더
        memberTable.addHeaderCell(createHeaderCell("번호"));
        memberTable.addHeaderCell(createHeaderCell("성명"));
        memberTable.addHeaderCell(createHeaderCell("학번"));
        memberTable.addHeaderCell(createHeaderCell("학과"));
        memberTable.addHeaderCell(createHeaderCell("연락처"));
        memberTable.addHeaderCell(createHeaderCell("직책"));
        memberTable.addHeaderCell(createHeaderCell("서명"));

        // 회원 데이터
        if (members != null && !members.isEmpty()) {
            int num = 1;
            for (Member member : members) {
                memberTable.addCell(createContentCell(String.valueOf(num++)));
                memberTable.addCell(createContentCell(member.getName()));
                memberTable.addCell(createContentCell(member.getStudentId()));
                memberTable.addCell(createContentCell(member.getDepartment() != null ? member.getDepartment() : ""));
                memberTable.addCell(createContentCell(member.getPhone() != null ? member.getPhone() : ""));
                memberTable.addCell(createContentCell(member.getRole() != null ? member.getRole() : "회원"));

                // 서명 셀
                Cell signCell = new Cell()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .setMinHeight(25);

                if (memberSignatures != null && memberSignatures.containsKey(member.getUserId())) {
                    try {
                        Bitmap sig = memberSignatures.get(member.getUserId());
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        sig.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        ImageData imageData = ImageDataFactory.create(stream.toByteArray());
                        Image signImage = new Image(imageData).setWidth(40).setHeight(20);
                        signCell.add(signImage);
                    } catch (Exception e) {
                        signCell.add(new Paragraph("").setFont(koreanFont));
                    }
                } else {
                    signCell.add(new Paragraph("").setFont(koreanFont));
                }
                memberTable.addCell(signCell);
            }
        } else {
            // 빈 행 추가 (최소 20행)
            for (int i = 1; i <= 20; i++) {
                memberTable.addCell(createContentCell(String.valueOf(i)));
                memberTable.addCell(createContentCell(""));
                memberTable.addCell(createContentCell(""));
                memberTable.addCell(createContentCell(""));
                memberTable.addCell(createContentCell(""));
                memberTable.addCell(createContentCell(""));
                memberTable.addCell(createContentCell(""));
            }
        }

        document.add(memberTable);

        // 총 인원
        Paragraph totalPara = new Paragraph("총 인원: " +
                (members != null ? members.size() : club.getMemberCount()) + "명")
                .setFont(koreanFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(10);
        document.add(totalPara);
    }

    /**
     * 페이지 5: 기구 조직표
     */
    private void addOrganizationChartPage(Document document) {
        Paragraph title = new Paragraph("기구 조직표")
                .setFont(koreanBoldFont)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30);
        document.add(title);

        Paragraph clubNamePara = new Paragraph("동아리명: " + club.getName())
                .setFont(koreanFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30);
        document.add(clubNamePara);

        // 조직도 테이블
        // 지도교수
        Table profTable = new Table(1).setWidth(UnitValue.createPercentValue(40))
                .setHorizontalAlignment(HorizontalAlignment.CENTER);
        profTable.addCell(createOrgCell("지도교수", professorName != null ? professorName : club.getProfessor()));
        document.add(profTable);

        // 연결선
        document.add(new Paragraph("|").setTextAlignment(TextAlignment.CENTER).setFontSize(20));

        // 회장
        Table presTable = new Table(1).setWidth(UnitValue.createPercentValue(40))
                .setHorizontalAlignment(HorizontalAlignment.CENTER);
        presTable.addCell(createOrgCell("회장", presidentName != null ? presidentName : ""));
        document.add(presTable);

        document.add(new Paragraph("|").setTextAlignment(TextAlignment.CENTER).setFontSize(20));

        // 부회장
        Table viceTable = new Table(1).setWidth(UnitValue.createPercentValue(40))
                .setHorizontalAlignment(HorizontalAlignment.CENTER);
        viceTable.addCell(createOrgCell("부회장", vicePresidentName != null ? vicePresidentName : ""));
        document.add(viceTable);

        document.add(new Paragraph("─────────────────────────────")
                .setTextAlignment(TextAlignment.CENTER).setFontSize(12));

        // 총무, 회계
        Table staffTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(80))
                .setHorizontalAlignment(HorizontalAlignment.CENTER);

        staffTable.addCell(createOrgCell("총무", secretaryName != null ? secretaryName : ""));
        staffTable.addCell(createOrgCell("회계", treasurerName != null ? treasurerName : ""));

        document.add(staffTable);

        document.add(new Paragraph("\n\n"));
        document.add(new Paragraph("─────────────────────────────")
                .setTextAlignment(TextAlignment.CENTER).setFontSize(12));

        // 일반 회원
        Paragraph membersPara = new Paragraph("회원")
                .setFont(koreanBoldFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10);
        document.add(membersPara);

        int memberCount = members != null ? members.size() : club.getMemberCount();
        Paragraph countPara = new Paragraph("(" + memberCount + "명)")
                .setFont(koreanFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(countPara);
    }

    /**
     * 회칙 페이지들 추가
     * 외부 PDF를 삽입할 때 현재 문서를 먼저 닫고 별도로 처리하지 않도록 수정
     */
    private void addClubRulesPages(Document document, PdfDocument pdfDoc) {
        Paragraph title = new Paragraph("운영 회칙")
                .setFont(koreanBoldFont)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        Paragraph clubNamePara = new Paragraph("동아리명: " + club.getName())
                .setFont(koreanFont)
                .setFontSize(12)
                .setMarginBottom(20);
        document.add(clubNamePara);

        // 외부 회칙 파일이 있으면 해당 내용을 삽입
        if (clubRulesUri != null) {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(clubRulesUri);
                if (inputStream != null) {
                    String mimeType = context.getContentResolver().getType(clubRulesUri);

                    if (mimeType != null && mimeType.equals("application/pdf")) {
                        // PDF 파일인 경우 - 내용을 텍스트로 변환하거나 이미지로 삽입
                        // 페이지 복사 방식 대신 안내 메시지 표시
                        document.add(new Paragraph("\n"));
                        document.add(new Paragraph("(별첨: 회칙 PDF 파일 첨부)")
                                .setFont(koreanFont)
                                .setFontSize(12)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setItalic());
                        document.add(new Paragraph("\n"));

                        // 기본 회칙 양식도 함께 표시
                        addDefaultRulesTemplate(document);
                    } else {
                        // 텍스트 파일인 경우
                        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                        int nRead;
                        byte[] data = new byte[16384];
                        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                            buffer.write(data, 0, nRead);
                        }
                        String rulesText = new String(buffer.toByteArray(), "UTF-8");

                        Paragraph rulesPara = new Paragraph(rulesText)
                                .setFont(koreanFont)
                                .setFontSize(10);
                        document.add(rulesPara);
                    }
                    inputStream.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "회칙 파일 로드 실패", e);
                addDefaultRulesTemplate(document);
            }
        } else {
            // 기본 회칙 템플릿
            addDefaultRulesTemplate(document);
        }
    }

    private void addDefaultRulesTemplate(Document document) {
        String[] defaultRules = {
                "제1장 총칙",
                "",
                "제1조(명칭) 본 동아리는 \"" + club.getName() + "\"라 칭한다.",
                "",
                "제2조(목적) 본 동아리는 다음의 목적을 가진다.",
                "  " + (club.getPurpose() != null ? club.getPurpose() : ""),
                "",
                "제3조(소재지) 본 동아리의 소재지는 나사렛대학교 내로 한다.",
                "",
                "제2장 회원",
                "",
                "제4조(자격) 본 동아리의 회원은 나사렛대학교 재학생으로 한다.",
                "",
                "제5조(가입) 본 동아리에 가입하고자 하는 자는 소정의 절차를 거쳐 가입할 수 있다.",
                "",
                "제6조(탈퇴) 회원은 자유로이 탈퇴할 수 있다.",
                "",
                "제3장 임원",
                "",
                "제7조(임원) 본 동아리에는 다음의 임원을 둔다.",
                "  1. 회장 1명",
                "  2. 부회장 1명",
                "  3. 총무 1명",
                "  4. 회계 1명",
                "",
                "제8조(임기) 임원의 임기는 1년으로 한다.",
                "",
                "제4장 회의",
                "",
                "제9조(정기회의) 정기회의는 매월 1회 개최한다.",
                "",
                "제10조(임시회의) 임시회의는 회장이 필요하다고 인정할 때 소집할 수 있다.",
                "",
                "제5장 재정",
                "",
                "제11조(재정) 본 동아리의 재정은 회비 및 지원금으로 충당한다.",
                "",
                "부칙",
                "",
                "제1조(시행일) 본 회칙은 공포한 날부터 시행한다."
        };

        for (String rule : defaultRules) {
            document.add(new Paragraph(rule)
                    .setFont(rule.startsWith("제") && rule.contains("장") ? koreanBoldFont : koreanFont)
                    .setFontSize(11)
                    .setMarginBottom(3));
        }
    }

    /**
     * 행사 결과 및 결산서
     */
    private void addEventResultsPage(Document document) {
        Paragraph title = new Paragraph("행사 결과 및 결산서")
                .setFont(koreanBoldFont)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        int prevYear = Integer.parseInt(academicYear) - 1;
        Paragraph yearPara = new Paragraph(prevYear + "학년도 활동 결산")
                .setFont(koreanFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(yearPara);

        // 결산 테이블
        Table resultTable = new Table(UnitValue.createPercentArray(new float[]{10, 25, 15, 25, 25}))
                .setWidth(UnitValue.createPercentValue(100));

        resultTable.addHeaderCell(createHeaderCell("번호"));
        resultTable.addHeaderCell(createHeaderCell("행사명"));
        resultTable.addHeaderCell(createHeaderCell("일시"));
        resultTable.addHeaderCell(createHeaderCell("지출 내역"));
        resultTable.addHeaderCell(createHeaderCell("금액"));

        long totalExpense = 0;
        if (activityResults != null && !activityResults.isEmpty()) {
            int num = 1;
            for (ActivityResult result : activityResults) {
                resultTable.addCell(createContentCell(String.valueOf(num++)));
                resultTable.addCell(createContentCell(result.eventName));
                resultTable.addCell(createContentCell(result.date));
                resultTable.addCell(createContentCell(result.expenseDetail));
                resultTable.addCell(createContentCell(String.format("%,d원", result.amount)));
                totalExpense += result.amount;
            }
        } else {
            // 빈 행
            for (int i = 1; i <= 10; i++) {
                resultTable.addCell(createContentCell(String.valueOf(i)));
                resultTable.addCell(createContentCell(""));
                resultTable.addCell(createContentCell(""));
                resultTable.addCell(createContentCell(""));
                resultTable.addCell(createContentCell(""));
            }
        }

        // 합계
        resultTable.addCell(createHeaderCellWithColspan("합계", 4));
        resultTable.addCell(createContentCell(String.format("%,d원", totalExpense)));

        document.add(resultTable);
    }

    /**
     * 활동 결과보고서
     */
    private void addActivityReportPages(Document document) {
        Paragraph title = new Paragraph("활동 결과보고서")
                .setFont(koreanBoldFont)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        // 활동 보고 테이블
        Table reportTable = new Table(UnitValue.createPercentArray(new float[]{15, 85}))
                .setWidth(UnitValue.createPercentValue(100));

        reportTable.addCell(createHeaderCell("행사명"));
        reportTable.addCell(createContentCell("").setMinHeight(30));

        reportTable.addCell(createHeaderCell("일시"));
        reportTable.addCell(createContentCell("").setMinHeight(30));

        reportTable.addCell(createHeaderCell("장소"));
        reportTable.addCell(createContentCell("").setMinHeight(30));

        reportTable.addCell(createHeaderCell("참석 인원"));
        reportTable.addCell(createContentCell("").setMinHeight(30));

        reportTable.addCell(createHeaderCell("활동 내용"));
        reportTable.addCell(createContentCell("").setMinHeight(200));

        reportTable.addCell(createHeaderCell("결과 및 평가"));
        reportTable.addCell(createContentCell("").setMinHeight(100));

        reportTable.addCell(createHeaderCell("활동 사진"));
        reportTable.addCell(createContentCell("").setMinHeight(200));

        document.add(reportTable);
    }

    /**
     * 행사계획 및 예산서
     */
    private void addEventPlanPage(Document document) {
        Paragraph title = new Paragraph("행사계획 및 예산서")
                .setFont(koreanBoldFont)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        Paragraph yearPara = new Paragraph(academicYear + "학년도 활동 계획")
                .setFont(koreanFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(yearPara);

        // 계획 테이블
        Table planTable = new Table(UnitValue.createPercentArray(new float[]{10, 25, 15, 25, 25}))
                .setWidth(UnitValue.createPercentValue(100));

        planTable.addHeaderCell(createHeaderCell("번호"));
        planTable.addHeaderCell(createHeaderCell("행사명"));
        planTable.addHeaderCell(createHeaderCell("예정일"));
        planTable.addHeaderCell(createHeaderCell("예산 내역"));
        planTable.addHeaderCell(createHeaderCell("예상 금액"));

        long totalBudget = 0;
        if (eventPlans != null && !eventPlans.isEmpty()) {
            int num = 1;
            for (EventPlan plan : eventPlans) {
                planTable.addCell(createContentCell(String.valueOf(num++)));
                planTable.addCell(createContentCell(plan.eventName));
                planTable.addCell(createContentCell(plan.plannedDate));
                planTable.addCell(createContentCell(plan.budgetDetail));
                planTable.addCell(createContentCell(String.format("%,d원", plan.estimatedAmount)));
                totalBudget += plan.estimatedAmount;
            }
        } else {
            // 빈 행
            for (int i = 1; i <= 10; i++) {
                planTable.addCell(createContentCell(String.valueOf(i)));
                planTable.addCell(createContentCell(""));
                planTable.addCell(createContentCell(""));
                planTable.addCell(createContentCell(""));
                planTable.addCell(createContentCell(""));
            }
        }

        // 합계
        planTable.addCell(createHeaderCellWithColspan("합계", 4));
        planTable.addCell(createContentCell(String.format("%,d원", totalBudget)));

        document.add(planTable);
    }

    /**
     * 월 사업/예산 세부계획서
     */
    private void addMonthlyPlanPage(Document document) {
        Paragraph title = new Paragraph("월 사업/예산 세부계획서")
                .setFont(koreanBoldFont)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        Paragraph clubNamePara = new Paragraph("동아리명: " + club.getName())
                .setFont(koreanFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(10);
        document.add(clubNamePara);

        // 월별 테이블 - 자동 크기 조절 (활동 계획 컬럼을 넓게)
        Table monthTable = new Table(UnitValue.createPercentArray(new float[]{10, 68, 22}))
                .setWidth(UnitValue.createPercentValue(100));

        monthTable.addHeaderCell(createHeaderCell("월"));
        monthTable.addHeaderCell(createHeaderCell("사업 계획 / 활동 내용"));
        monthTable.addHeaderCell(createHeaderCell("예산 (원)"));

        // 학기 순서로 표시 (3월~12월, 1월~2월)
        int[] monthOrder = {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 1, 2};
        String[] monthLabels = {"3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월", "1월", "2월"};

        long totalBudget = 0;

        for (int i = 0; i < monthOrder.length; i++) {
            String monthKey = String.valueOf(monthOrder[i]);
            monthTable.addCell(createContentCell(monthLabels[i]));

            // 월별 계획 표시
            String plan = "";
            if (monthlyPlans != null && monthlyPlans.containsKey(monthKey)) {
                plan = monthlyPlans.get(monthKey);
            }
            if (plan == null || plan.isEmpty()) {
                // 기존 club 데이터에서도 확인
                String schedule = club.getScheduleForMonth(monthOrder[i]);
                if (schedule != null && !schedule.isEmpty()) {
                    plan = schedule;
                }
            }
            // 셀 높이를 텍스트 길이에 맞게 조절 (최소 높이 설정)
            Cell planCell = createAutoResizeContentCell(plan);
            monthTable.addCell(planCell);

            // 월별 예산 표시
            long budget = 0;
            if (monthlyBudgets != null && monthlyBudgets.containsKey(monthKey)) {
                budget = monthlyBudgets.get(monthKey);
            }
            totalBudget += budget;

            String budgetStr = budget > 0 ? java.text.NumberFormat.getNumberInstance(Locale.KOREA).format(budget) : "";
            monthTable.addCell(createContentCell(budgetStr).setTextAlignment(TextAlignment.RIGHT));
        }

        // 합계 행
        monthTable.addCell(createHeaderCell("합계").setBackgroundColor(new DeviceRgb(230, 230, 230)));
        monthTable.addCell(createContentCell("").setBackgroundColor(new DeviceRgb(230, 230, 230)));
        monthTable.addCell(createContentCell(java.text.NumberFormat.getNumberInstance(Locale.KOREA).format(totalBudget))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBackgroundColor(new DeviceRgb(230, 230, 230))
                .setFont(koreanBoldFont));

        document.add(monthTable);
    }

    /**
     * 자동 높이 조절 셀 생성 - 텍스트 내용에 맞게 셀 크기 자동 조절
     */
    private Cell createAutoResizeContentCell(String text) {
        String displayText = (text != null && !text.trim().isEmpty()) ? text : "";

        Cell cell = new Cell()
                .add(new Paragraph(displayText).setFont(koreanFont).setFontSize(10).setMultipliedLeading(1.3f))
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                .setPadding(6)
                .setVerticalAlignment(VerticalAlignment.TOP); // 위쪽 정렬로 변경

        // 텍스트 길이와 줄바꿈에 따라 최소 높이 동적 설정
        if (displayText.isEmpty()) {
            cell.setMinHeight(25);
        } else {
            // 줄바꿈 수 계산
            int lineBreaks = displayText.split("\n").length;
            // 예상 줄 수 계산 (약 25자당 1줄)
            int estimatedLines = Math.max(lineBreaks, (displayText.length() / 25) + 1);

            // 줄 수에 따라 높이 설정 (줄당 약 15px)
            int minHeight = Math.max(25, estimatedLines * 15);
            // 최대 높이 제한 (페이지를 넘기지 않도록)
            minHeight = Math.min(minHeight, 150);

            cell.setMinHeight(minHeight);
        }

        return cell;
    }

    /**
     * 동아리실 사용 서약서
     */
    private void addClubRoomAgreementPage(Document document) {
        Paragraph title = new Paragraph("동아리실 사용 서약서")
                .setFont(koreanBoldFont)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30);
        document.add(title);

        String agreementText =
                "본인(들)은 " + club.getName() + " 동아리의 회원으로서 동아리실을 사용함에 있어 " +
                "다음 사항을 준수할 것을 서약합니다.\n\n" +
                "1. 동아리실 내에서 음주, 흡연, 도박 등 불건전한 행위를 하지 않겠습니다.\n\n" +
                "2. 동아리실을 청결하게 유지하고, 사용 후에는 정리정돈을 하겠습니다.\n\n" +
                "3. 동아리실 내 비품 및 시설물을 소중히 다루고, 파손 시 즉시 보고하겠습니다.\n\n" +
                "4. 야간 및 주말 사용 시 학교 규정을 준수하겠습니다.\n\n" +
                "5. 동아리실을 동아리 활동 목적 외에 사용하지 않겠습니다.\n\n" +
                "6. 외부인의 동아리실 무단 출입을 금지하겠습니다.\n\n" +
                "7. 위 사항을 위반 시 동아리실 사용 권한이 박탈될 수 있음을 인지합니다.\n\n";

        Paragraph agreementPara = new Paragraph(agreementText)
                .setFont(koreanFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.LEFT);
        document.add(agreementPara);

        document.add(new Paragraph("\n\n"));

        // 날짜
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        Paragraph datePara = new Paragraph(sdf.format(new Date()))
                .setFont(koreanFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(datePara);

        document.add(new Paragraph("\n\n"));

        // 서명 영역
        Table signTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(80))
                .setHorizontalAlignment(HorizontalAlignment.CENTER);

        signTable.addCell(new Cell()
                .add(new Paragraph("동아리명: " + club.getName()).setFont(koreanFont))
                .setBorder(Border.NO_BORDER));
        signTable.addCell(new Cell().setBorder(Border.NO_BORDER));

        signTable.addCell(new Cell()
                .add(new Paragraph("대표자: " + (presidentName != null ? presidentName : "")).setFont(koreanFont))
                .setBorder(Border.NO_BORDER));
        signTable.addCell(new Cell()
                .add(new Paragraph("(인)").setFont(koreanFont))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT));

        document.add(signTable);

        document.add(new Paragraph("\n\n\n"));

        Paragraph receiverPara = new Paragraph("나사렛대학교 총학생회장 귀하")
                .setFont(koreanBoldFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(receiverPara);
    }

    // 유틸리티 메서드들

    private Cell createCell(String text, PdfFont font, int fontSize, TextAlignment alignment, boolean hasBorder) {
        Cell cell = new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(fontSize))
                .setTextAlignment(alignment)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(10);

        if (!hasBorder) {
            cell.setBorder(Border.NO_BORDER);
        }
        return cell;
    }

    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setFont(koreanBoldFont).setFontSize(10))
                .setBackgroundColor(new DeviceRgb(240, 240, 240))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(5);
    }

    private Cell createContentCell(String text) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "").setFont(koreanFont).setFontSize(10))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(5);
    }

    private Cell createContentCellWithColspan(String text, int colspan) {
        return new Cell(1, colspan)
                .add(new Paragraph(text != null ? text : "").setFont(koreanFont).setFontSize(10))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(5);
    }

    private Cell createHeaderCellWithColspan(String text, int colspan) {
        return new Cell(1, colspan)
                .add(new Paragraph(text).setFont(koreanBoldFont).setFontSize(10))
                .setBackgroundColor(new DeviceRgb(240, 240, 240))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(5);
    }

    private Cell createOrgCell(String title, String name) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(1))
                .setPadding(10)
                .setTextAlignment(TextAlignment.CENTER);

        cell.add(new Paragraph(title).setFont(koreanBoldFont).setFontSize(12));
        cell.add(new Paragraph(name != null ? name : "").setFont(koreanFont).setFontSize(11));

        return cell;
    }

    // 데이터 클래스들

    public static class ActivityResult {
        public String eventName;
        public String date;
        public String expenseDetail;
        public long amount;

        public ActivityResult(String eventName, String date, String expenseDetail, long amount) {
            this.eventName = eventName;
            this.date = date;
            this.expenseDetail = expenseDetail;
            this.amount = amount;
        }
    }

    public static class EventPlan {
        public String eventName;
        public String plannedDate;
        public String budgetDetail;
        public long estimatedAmount;

        public EventPlan(String eventName, String plannedDate, String budgetDetail, long estimatedAmount) {
            this.eventName = eventName;
            this.plannedDate = plannedDate;
            this.budgetDetail = budgetDetail;
            this.estimatedAmount = estimatedAmount;
        }
    }

    public static class BudgetItem {
        public String category;
        public String description;
        public long amount;

        public BudgetItem(String category, String description, long amount) {
            this.category = category;
            this.description = description;
            this.amount = amount;
        }
    }
}
