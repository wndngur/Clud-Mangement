# Firebase 설정 가이드

## 1. Firebase 프로젝트 생성

1. [Firebase Console](https://console.firebase.google.com/)에 접속
2. "프로젝트 추가" 클릭
3. 프로젝트 이름 입력 (예: ClubManagement)
4. Google 애널리틱스 설정 (선택사항)
5. 프로젝트 생성 완료

## 2. Android 앱 추가

1. Firebase 프로젝트 개요에서 "Android 앱 추가" 클릭
2. Android 패키지 이름 입력: `com.example.clubmanagement`
3. 앱 닉네임 입력 (선택사항): Club Management
4. 디버그 서명 인증서 SHA-1 입력 (선택사항)
   ```bash
   # Windows
   keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

   # Mac/Linux
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```

## 3. google-services.json 다운로드

1. Firebase Console에서 `google-services.json` 파일 다운로드
2. 다운로드한 파일을 `app/` 폴더에 배치
   ```
   ClubManagement/
   └── app/
       ├── google-services.json  ← 여기에 배치
       └── build.gradle.kts
   ```

## 4. Firebase Authentication 설정

1. Firebase Console > Authentication > 시작하기
2. "로그인 방법" 탭 선택
3. **익명** 로그인 활성화 (테스트용)
   - 익명 옆의 스위치를 켜기
   - 저장

4. (선택사항) 이메일/비밀번호 로그인 활성화
   - 이메일/비밀번호 옆의 스위치를 켜기
   - 저장

## 5. Cloud Firestore 설정

1. Firebase Console > Firestore Database > 데이터베이스 만들기
2. **테스트 모드로 시작** 선택 (개발용)
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /{document=**} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```
   > **주의**: 프로덕션 환경에서는 보안 규칙을 강화해야 합니다!

3. 위치 선택: asia-northeast3 (서울) 또는 asia-northeast1 (도쿄)
4. 완료

## 6. Firebase Storage 설정

1. Firebase Console > Storage > 시작하기
2. **테스트 모드로 시작** 선택
   ```
   rules_version = '2';
   service firebase.storage {
     match /b/{bucket}/o {
       match /{allPaths=**} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```
3. 위치 선택: asia-northeast3 (서울) 또는 asia-northeast1 (도쿄)
4. 완료

## 7. Firestore 보안 규칙 (프로덕션용)

프로덕션 환경에서는 다음과 같은 보안 규칙을 사용하세요:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // 서명 컬렉션: 본인의 서명만 읽기/쓰기 가능
    match /signatures/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }

    // 문서 컬렉션: 인증된 사용자는 읽기 가능, 작성자만 쓰기 가능
    match /documents/{docId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null &&
                              request.auth.uid == resource.data.creatorId;
    }

    // 관리자 전용 컬렉션
    match /admin/{document=**} {
      allow read, write: if request.auth != null &&
                           get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
  }
}
```

## 8. Storage 보안 규칙 (프로덕션용)

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {

    // 서명 이미지: 본인의 서명만 읽기/쓰기 가능
    match /signatures/{userId}/{fileName} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }

    // 문서 PDF: 인증된 사용자만 읽기 가능
    match /documents/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

## 9. 앱 빌드 및 실행

1. Android Studio에서 프로젝트 열기
2. Gradle Sync 실행
3. 앱 실행

## 10. Firebase Console에서 확인

앱을 실행한 후 다음을 확인하세요:

1. **Authentication > 사용자**
   - 익명 사용자가 생성되었는지 확인

2. **Firestore Database**
   - `signatures` 컬렉션에 서명 데이터가 저장되는지 확인
   - `documents` 컬렉션에 문서 데이터가 저장되는지 확인

3. **Storage**
   - `signatures/{userId}/` 폴더에 서명 이미지가 업로드되는지 확인

## 트러블슈팅

### google-services.json 오류
- `google-services.json` 파일이 `app/` 폴더에 있는지 확인
- 패키지 이름이 `com.example.clubmanagement`와 일치하는지 확인

### Authentication 오류
- Firebase Console에서 익명 로그인이 활성화되어 있는지 확인
- 인터넷 연결 확인

### Firestore 권한 오류
- Firestore 보안 규칙이 올바르게 설정되어 있는지 확인
- 사용자가 인증되었는지 확인 (`request.auth != null`)

### Storage 업로드 실패
- Storage 보안 규칙 확인
- 파일 크기 제한 확인 (기본 5MB)
- 인터넷 연결 및 권한 확인
