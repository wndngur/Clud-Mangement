# Cloud Functions 설정 가이드

## 개요

Cloud Functions를 사용하여 다음 기능을 서버 측에서 처리할 수 있습니다:

1. **서명 이미지 자동 처리**: 업로드된 서명 이미지의 배경 자동 제거
2. **PDF 생성**: 서버에서 PDF 생성 및 Storage 업로드
3. **알림 발송**: 서명 등록 완료 시 알림 발송

> **참고**: 현재 앱은 클라이언트 측에서 모든 처리를 수행합니다. Cloud Functions는 선택사항이며,
> 더 나은 성능과 보안이 필요한 경우에 사용하세요.

## 1. Firebase CLI 설치

```bash
npm install -g firebase-tools
```

## 2. Firebase 로그인

```bash
firebase login
```

## 3. Functions 초기화

프로젝트 루트 디렉토리에서:

```bash
firebase init functions
```

선택사항:
- 언어: **JavaScript** 또는 **TypeScript**
- ESLint: Yes
- Dependencies 설치: Yes

## 4. Cloud Functions 코드 작성

`functions/index.js` (JavaScript) 또는 `functions/src/index.ts` (TypeScript):

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { Storage } = require('@google-cloud/storage');
const sharp = require('sharp'); // 이미지 처리 라이브러리

admin.initializeApp();

// ========================================
// 1. 서명 이미지 배경 제거 함수
// ========================================

exports.processSignatureImage = functions.storage
  .object()
  .onFinalize(async (object) => {
    const filePath = object.name; // 예: signatures/userId/image.png
    const contentType = object.contentType;

    // 서명 이미지만 처리
    if (!filePath.startsWith('signatures/') || !contentType.startsWith('image/')) {
      console.log('서명 이미지가 아님, 처리 건너뜀');
      return null;
    }

    // 이미 처리된 이미지는 건너뜀
    if (filePath.includes('_clean')) {
      console.log('이미 처리된 이미지');
      return null;
    }

    try {
      const bucket = admin.storage().bucket(object.bucket);
      const file = bucket.file(filePath);

      // 이미지 다운로드
      const [imageBuffer] = await file.download();

      // Sharp를 사용한 배경 제거 (흰색 배경 -> 투명)
      const processedBuffer = await sharp(imageBuffer)
        .flatten({ background: { r: 255, g: 255, b: 255, alpha: 0 } })
        .threshold(240) // 밝기 240 이상 픽셀 제거
        .png()
        .toBuffer();

      // 처리된 이미지 저장
      const cleanFilePath = filePath.replace(/\.[^.]+$/, '_clean.png');
      const cleanFile = bucket.file(cleanFilePath);

      await cleanFile.save(processedBuffer, {
        metadata: {
          contentType: 'image/png',
        },
      });

      console.log(`배경 제거 완료: ${cleanFilePath}`);

      // Firestore 업데이트
      const userId = filePath.split('/')[1];
      const cleanUrl = await cleanFile.getSignedUrl({
        action: 'read',
        expires: '03-09-2491',
      });

      await admin.firestore()
        .collection('signatures')
        .doc(userId)
        .update({
          signatureImageUrl: cleanUrl[0],
          lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        });

      return null;
    } catch (error) {
      console.error('이미지 처리 실패:', error);
      return null;
    }
  });

// ========================================
// 2. PDF 생성 함수 (서버 측)
// ========================================

const PDFDocument = require('pdfkit');

exports.generateDocumentPdf = functions.https.onCall(async (data, context) => {
  // 인증 확인
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      '로그인이 필요합니다.'
    );
  }

  const { documentId } = data;

  try {
    // Firestore에서 문서 데이터 가져오기
    const docRef = admin.firestore().collection('documents').doc(documentId);
    const docSnapshot = await docRef.get();

    if (!docSnapshot.exists) {
      throw new functions.https.HttpsError('not-found', '문서를 찾을 수 없습니다.');
    }

    const documentData = docSnapshot.data();

    // PDF 생성
    const pdfBuffer = await createPdf(documentData);

    // Storage에 업로드
    const bucket = admin.storage().bucket();
    const fileName = `documents/${documentId}.pdf`;
    const file = bucket.file(fileName);

    await file.save(pdfBuffer, {
      metadata: {
        contentType: 'application/pdf',
      },
    });

    // 다운로드 URL 생성
    const [url] = await file.getSignedUrl({
      action: 'read',
      expires: '03-09-2491',
    });

    // Firestore 업데이트
    await docRef.update({
      pdfUrl: url,
      signaturePlaced: true,
    });

    return { pdfUrl: url };
  } catch (error) {
    console.error('PDF 생성 실패:', error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});

async function createPdf(documentData) {
  return new Promise((resolve, reject) => {
    const doc = new PDFDocument();
    const chunks = [];

    doc.on('data', (chunk) => chunks.push(chunk));
    doc.on('end', () => resolve(Buffer.concat(chunks)));
    doc.on('error', reject);

    // PDF 내용 작성
    doc.fontSize(20).text(documentData.title, { align: 'center' });
    doc.moveDown();
    doc.fontSize(12).text(documentData.content);
    doc.moveDown(2);

    if (documentData.requiresSignature) {
      doc.text('서명:', { continued: false });
      // TODO: 서명 이미지 추가
    }

    doc.end();
  });
}

// ========================================
// 3. 서명 등록 알림 함수
// ========================================

exports.notifySignatureRegistered = functions.firestore
  .document('signatures/{userId}')
  .onCreate(async (snap, context) => {
    const userId = context.params.userId;
    const signatureData = snap.data();

    console.log(`새 서명 등록: ${userId}`);

    // TODO: FCM 푸시 알림 발송
    // TODO: 이메일 알림 발송

    return null;
  });

// ========================================
// 4. 서명 미등록 사용자 알림 (스케줄링)
// ========================================

exports.notifyMissingSignatures = functions.pubsub
  .schedule('0 9 * * 1') // 매주 월요일 오전 9시
  .timeZone('Asia/Seoul')
  .onRun(async (context) => {
    // 모든 사용자 조회
    const usersSnapshot = await admin.firestore().collection('users').get();

    // 서명이 없는 사용자 찾기
    const missingSignatures = [];

    for (const userDoc of usersSnapshot.docs) {
      const userId = userDoc.id;
      const signatureDoc = await admin.firestore()
        .collection('signatures')
        .doc(userId)
        .get();

      if (!signatureDoc.exists) {
        missingSignatures.push(userId);
      }
    }

    console.log(`서명 미등록 사용자: ${missingSignatures.length}명`);

    // TODO: 알림 발송

    return null;
  });
```

## 5. 필요한 패키지 설치

```bash
cd functions
npm install sharp pdfkit @google-cloud/storage
```

## 6. Functions 배포

```bash
firebase deploy --only functions
```

특정 함수만 배포:
```bash
firebase deploy --only functions:processSignatureImage
```

## 7. Android 앱에서 호출

### 7.1. Callable Function 호출 예제

```java
// DocumentGenerateActivity.java

private void generatePdfOnServer(String documentId) {
    FirebaseFunctions functions = FirebaseFunctions.getInstance();

    Map<String, Object> data = new HashMap<>();
    data.put("documentId", documentId);

    functions
        .getHttpsCallable("generateDocumentPdf")
        .call(data)
        .addOnSuccessListener(result -> {
            Map<String, Object> resultData = (Map<String, Object>) result.getData();
            String pdfUrl = (String) resultData.get("pdfUrl");

            Toast.makeText(this, "PDF 생성 완료: " + pdfUrl,
                Toast.LENGTH_LONG).show();
        })
        .addOnFailureListener(e -> {
            Toast.makeText(this, "PDF 생성 실패: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        });
}
```

## 8. 비용 고려사항

Cloud Functions는 다음과 같은 경우 비용이 발생합니다:

- **실행 시간**: 함수가 실행되는 시간에 따라 과금
- **네트워크**: Storage 다운로드/업로드 데이터 전송량
- **호출 횟수**: 함수 호출 횟수

**무료 할당량** (Spark 플랜):
- 호출: 2,000,000회/월
- GB-초: 400,000 GB-초/월
- CPU-초: 200,000 CPU-초/월
- 아웃바운드 네트워크: 5GB/월

**Blaze 플랜 권장**: 프로덕션 환경에서는 Blaze 플랜 사용

## 9. 모니터링

Firebase Console > Functions에서 다음을 확인:

- 함수 실행 로그
- 오류 발생 현황
- 실행 시간 및 메모리 사용량
- 비용

## 10. 로컬 테스트

```bash
# Firebase Emulator 실행
firebase emulators:start

# 특정 emulator만 실행
firebase emulators:start --only functions,firestore
```

앱에서 emulator 사용:

```java
// MainActivity.java (onCreate)
if (BuildConfig.DEBUG) {
    FirebaseFunctions.getInstance().useEmulator("10.0.2.2", 5001);
    FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080);
}
```

## 트러블슈팅

### sharp 설치 실패
```bash
npm install --platform=linux --arch=x64 sharp
```

### Functions 배포 실패
- Firebase 프로젝트가 Blaze 플랜인지 확인
- `firebase-admin` SDK 버전 확인
- Node.js 버전 확인 (권장: Node 18)

### 권한 오류
Firebase Console > IAM에서 Service Account 권한 확인
