# 빠른 시작 가이드 (Quick Start Guide)

이 가이드는 처음부터 앱을 실행하기까지의 최소 단계를 설명합니다.

## 1. Firebase 프로젝트 설정 (5분)

### 1.1. Firebase Console에서 프로젝트 생성
1. https://console.firebase.google.com/ 접속
2. "프로젝트 추가" 클릭
3. 프로젝트 이름: `ClubManagement` 입력
4. 계속 클릭하여 프로젝트 생성 완료

### 1.2. Android 앱 추가
1. Firebase 프로젝트 개요 > Android 앱 추가
2. Android 패키지 이름: `com.example.clubmanagement` 입력
3. "앱 등록" 클릭
4. `google-services.json` 다운로드
5. 다운로드한 파일을 `app/` 폴더에 복사

### 1.3. Firebase 서비스 활성화

**Authentication 활성화**:
1. Firebase Console > Authentication > 시작하기
2. 로그인 방법 > 익명 > 스위치 켜기 > 저장

**Firestore 활성화**:
1. Firebase Console > Firestore Database > 데이터베이스 만들기
2. **테스트 모드로 시작** 선택
3. 위치: `asia-northeast3` (서울) 선택
4. 완료

**Storage 활성화**:
1. Firebase Console > Storage > 시작하기
2. **테스트 모드로 시작** 선택
3. 완료

## 2. Android Studio에서 프로젝트 열기

1. Android Studio 실행
2. File > Open
3. `ClubManagement` 폴더 선택
4. Gradle Sync 완료 대기 (수 분 소요)

## 3. 앱 실행

1. 에뮬레이터 또는 실제 기기 연결
2. Run 버튼 (▶️) 클릭
3. 앱이 설치되고 실행됩니다

## 4. 기능 테스트

### 서명 등록하기
1. 홈 화면에서 "📝 서명 패드" 선택
2. 화면에 서명 작성
3. "저장" 클릭

### 문서 생성하기
1. 홈 화면에서 "📄 문서 생성" 선택
2. 문서 유형: "활동 보고서" 선택
3. 제목: "테스트 활동" 입력
4. 내용: "테스트 내용입니다" 입력
5. "서명 포함" 체크박스 확인
6. "PDF 생성" 클릭

생성된 PDF는 `Documents/ClubManagement/` 폴더에 저장됩니다.

## 5. Firebase Console에서 확인

### 5.1. Authentication 확인
1. Firebase Console > Authentication > Users
2. 익명 사용자가 생성되었는지 확인

### 5.2. Firestore 확인
1. Firebase Console > Firestore Database > Data
2. `signatures` 컬렉션이 생성되었는지 확인
3. 서명 데이터가 저장되었는지 확인

### 5.3. Storage 확인
1. Firebase Console > Storage > Files
2. `signatures/{userId}/` 폴더가 생성되었는지 확인
3. 서명 이미지 파일이 업로드되었는지 확인

## 문제 해결

### 빌드 오류: "google-services.json not found"
- `google-services.json` 파일이 `app/` 폴더에 있는지 확인
- 파일 이름이 정확한지 확인 (`.json.template` 아님)

### 런타임 오류: "Authentication failed"
- Firebase Console에서 익명 로그인이 활성화되어 있는지 확인
- 인터넷에 연결되어 있는지 확인

### Firestore 권한 오류
- Firestore가 **테스트 모드**로 설정되어 있는지 확인
- Firebase Console > Firestore > Rules에서 확인:
  ```
  allow read, write: if request.time < timestamp...
  ```

### Storage 업로드 실패
- Storage가 **테스트 모드**로 설정되어 있는지 확인
- 앱 권한 설정 확인 (설정 > 앱 > ClubManagement > 권한)

## 다음 단계

- 자세한 설정: [FIREBASE_SETUP.md](FIREBASE_SETUP.md)
- Cloud Functions 사용: [CLOUD_FUNCTIONS_SETUP.md](CLOUD_FUNCTIONS_SETUP.md)
- 전체 문서: [README.md](README.md)

## 지원

문제가 해결되지 않으면 다음을 확인하세요:
1. Android Studio 버전 업데이트
2. Gradle Clean & Rebuild
3. 에뮬레이터 재시작
4. Firebase 프로젝트 설정 재확인
