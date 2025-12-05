# ClubManagement 앱 Activity 설명서

> **작성자:** 김준성 (2010045)
> **작성일:** 2025년 12월 5일
> **프로젝트:** 대학 동아리 관리 애플리케이션

---

## 목차

1. [인증 및 로그인](#1-인증-및-로그인)
2. [메인 화면](#2-메인-화면)
3. [동아리 관리](#3-동아리-관리)
4. [부원 관리](#4-부원-관리)
5. [공지사항](#5-공지사항)
6. [Q&A 및 채팅](#6-qa-및-채팅)
7. [공금 관리](#7-공금-관리)
8. [서명 및 문서](#8-서명-및-문서)
9. [설정](#9-설정)
10. [기타](#10-기타)

---

## 1. 인증 및 로그인

### LoginActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.LoginActivity` |
| **용도** | 사용자 로그인 화면 |
| **설명** | 이메일과 비밀번호를 입력하여 Firebase Authentication으로 로그인합니다. 자동 로그인 기능을 지원하며, 로그인 성공 시 메인 화면으로 이동합니다. 앱의 진입점(Launcher Activity)입니다. |

### SignUpActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.SignUpActivity` |
| **용도** | 일반 사용자 회원가입 화면 |
| **설명** | 신규 사용자가 이메일, 비밀번호, 이름, 학번, 학과, 전화번호 등의 정보를 입력하여 계정을 생성합니다. Firebase Authentication에 사용자를 등록하고 Firestore에 사용자 정보를 저장합니다. |

### AdminLoginActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.AdminLoginActivity` |
| **용도** | 관리자 로그인 화면 |
| **설명** | 동아리 관리자 또는 최고 관리자가 별도의 관리자 비밀번호를 입력하여 관리자 권한으로 로그인합니다. 일반 로그인과 분리된 관리자 전용 인증 화면입니다. |

### AdminSignUpActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.AdminSignUpActivity` |
| **용도** | 관리자 회원가입 화면 |
| **설명** | 관리자 권한이 필요한 사용자가 관리자 계정을 생성합니다. 관리자 비밀번호 인증을 거쳐 관리자 권한을 부여받습니다. |

### LoginSettingsActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.LoginSettingsActivity` |
| **용도** | 로그인 옵션 선택 화면 |
| **설명** | 일반 로그인과 관리자 로그인 중 선택하는 화면입니다. 사용자 유형에 따라 적절한 로그인 화면으로 이동합니다. |

---

## 2. 메인 화면

### MainActivityNew
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.MainActivityNew` |
| **용도** | 앱 메인 화면 (일반 사용자) |
| **설명** | 로그인 후 표시되는 메인 화면입니다. 캐러셀 형태로 동아리 홍보 이미지를 슬라이드로 보여주고, 전역 배너와 공지사항을 표시합니다. 하단 네비게이션 바를 통해 홈, 채팅, 일반동아리, 추천, 내정보 화면으로 이동할 수 있습니다. 가입 승인 알림도 이 화면에서 처리됩니다. |

### AdminMainActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.AdminMainActivity` |
| **용도** | 관리자 메인 화면 |
| **설명** | 최고 관리자 모드로 로그인 시 표시되는 메인 화면입니다. 동아리 설립 승인, 삭제, 중앙동아리 관리 등 관리자 전용 기능에 접근할 수 있습니다. |

### MainActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.MainActivity` |
| **용도** | 기존 메인 화면 (미사용) |
| **설명** | 초기 개발 단계의 메인 화면으로, 현재는 MainActivityNew로 대체되었습니다. |

---

## 3. 동아리 관리

### ClubMainActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.ClubMainActivity` |
| **용도** | 동아리 메인 페이지 |
| **설명** | 특정 동아리의 메인 화면입니다. 동아리 배너 슬라이드, 공지사항 목록, 공금 현황 진행바, 부원 현황 진행바, 월별 일정을 표시합니다. 동아리 관리자는 공지사항 작성, 배너 관리, 부원 관리, 공금 관리 기능에 접근할 수 있습니다. |

### ClubListActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.ClubListActivity` |
| **용도** | 일반동아리 목록 화면 |
| **설명** | 모든 일반동아리 목록을 표시합니다. 동아리 검색 및 필터링 기능을 제공하며, 동아리를 선택하면 상세 정보 화면으로 이동합니다. |

### ClubRecommendActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.ClubRecommendActivity` |
| **용도** | 동아리 추천 화면 |
| **설명** | 사용자 선호도(분위기, 활동 유형, 목적)를 입력받아 알고리즘 기반으로 동아리를 추천합니다. 추천 점수가 높은 순서대로 동아리를 정렬하여 표시합니다. |

### DetailActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.DetailActivity` |
| **용도** | 동아리 상세 정보 화면 |
| **설명** | 동아리의 상세 정보를 표시합니다. 이미지 캐러셀, 지도교수, 학과, 위치, 설립일 등의 정보와 함께 가입 신청 버튼, Q&A 링크를 제공합니다. 최고 관리자 모드일 경우 특별 표시가 됩니다. |

### ClubInfoActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.ClubInfoActivity` |
| **용도** | 동아리 정보 조회 화면 |
| **설명** | 동아리의 기본 정보를 조회하는 화면입니다. 동아리 설명, 목적, 활동 계획 등을 확인할 수 있습니다. |

### ClubInfoEditActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.ClubInfoEditActivity` |
| **용도** | 동아리 정보 수정 화면 |
| **설명** | 동아리 관리자가 동아리 정보를 수정하는 화면입니다. 동아리명, 설명, 목적, 활동 계획, 위치 등을 편집할 수 있습니다. |

### ClubDescriptionEditActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.ClubDescriptionEditActivity` |
| **용도** | 동아리 설명 편집 화면 |
| **설명** | 동아리 설명 텍스트를 편집하는 전용 화면입니다. 긴 텍스트 입력을 위한 넓은 편집 영역을 제공합니다. |

### ClubSettingsActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.ClubSettingsActivity` |
| **용도** | 동아리 설정 화면 |
| **설명** | 동아리 관리자가 동아리 설정을 관리하는 화면입니다. 가입 신청 기간 설정, 공지사항 댓글 권한 설정, 배너 관리 등의 기능을 제공합니다. |

### ClubEstablishActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.ClubEstablishActivity` |
| **용도** | 동아리 설립 신청 화면 |
| **설명** | 새로운 동아리 설립을 신청하는 화면입니다. 동아리명, 설명, 목적, 활동 계획 등을 입력하여 최고 관리자에게 승인 요청을 보냅니다. |

### ClubApprovalListActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.ClubApprovalListActivity` |
| **용도** | 동아리 설립 승인 관리 화면 |
| **설명** | 최고 관리자가 동아리 설립 신청을 승인하거나 거절하는 화면입니다. 대기 중인 설립 신청 목록을 표시하고 각각에 대해 승인/거절 처리를 할 수 있습니다. |

### ClubDeleteActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.ClubDeleteActivity` |
| **용도** | 동아리 삭제 관리 화면 |
| **설명** | 최고 관리자가 동아리를 삭제하는 화면입니다. 삭제할 동아리를 선택하고 삭제 사유를 입력하여 동아리를 완전히 제거합니다. |

### MyClubsActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.MyClubsActivity` |
| **용도** | 내 동아리 목록 화면 |
| **설명** | 현재 로그인한 사용자가 가입한 동아리 목록을 표시합니다. 중앙동아리와 일반동아리를 구분하여 보여줍니다. |

### CentralApplicationsActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.CentralApplicationsActivity` |
| **용도** | 중앙동아리 신청 목록 화면 |
| **설명** | 중앙동아리 승격 신청 목록을 표시합니다. 일반동아리가 중앙동아리로 승격하기 위한 신청 내역을 확인할 수 있습니다. |

### CentralClubApplicationsActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.CentralClubApplicationsActivity` |
| **용도** | 중앙동아리 신청 관리 화면 |
| **설명** | 최고 관리자가 중앙동아리 승격 신청을 관리하는 화면입니다. 신청서 내용, 부원 수, 설립일 경과 등을 확인하고 승인/거절 처리를 합니다. |

### DemoteCentralClubActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.DemoteCentralClubActivity` |
| **용도** | 중앙동아리 강등 관리 화면 |
| **설명** | 최고 관리자가 중앙동아리를 일반동아리로 강등시키는 화면입니다. 부원 수 15명 미만 등 유지 조건을 충족하지 못하는 동아리를 강등 처리합니다. |

### CarouselEditActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.CarouselEditActivity` |
| **용도** | 캐러셀 이미지 편집 화면 |
| **설명** | 메인 화면이나 동아리 화면의 캐러셀 이미지를 편집하는 화면입니다. 이미지 추가, 삭제, 순서 변경, 배경색 설정 등을 할 수 있습니다. |

### EditRequestsActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.EditRequestsActivity` |
| **용도** | 수정 요청 목록 화면 |
| **설명** | 동아리 정보 수정 요청 목록을 표시합니다. 관리자가 요청된 수정 사항을 검토하고 승인/거절 처리를 합니다. |

---

## 4. 부원 관리

### MemberManagementActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.MemberManagementActivity` |
| **용도** | 부원 관리 화면 |
| **설명** | 동아리 관리자가 부원을 관리하는 화면입니다. 부원 목록, 가입 요청, 탈퇴 요청을 탭으로 구분하여 표시합니다. 부원 추가/삭제, 역할 설정(회장, 부회장, 총무, 회계, 회원), 관리자 권한 부여/해제, 퇴출 처리 등의 기능을 제공합니다. |

### MemberListActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.MemberListActivity` |
| **용도** | 부원 목록 공개 조회 화면 |
| **설명** | 동아리 부원 목록을 공개적으로 조회하는 화면입니다. 부원의 이름, 학번, 학과, 생일 등의 정보를 표시합니다. 관리 기능 없이 조회만 가능합니다. |

### MemberRegistrationActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.MemberRegistrationActivity` |
| **용도** | 부원 가입 신청 화면 |
| **설명** | 일반 사용자가 동아리에 가입 신청하는 화면입니다. 학번, 이름, 학과 등의 정보를 입력하고 서명을 업로드하여 가입 신청을 제출합니다. 신청 상태도 이 화면에서 확인할 수 있습니다. |

---

## 5. 공지사항

### NoticeListActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.NoticeListActivity` |
| **용도** | 공지사항 목록 화면 |
| **설명** | 동아리 공지사항 목록을 표시합니다. 고정된 공지는 상단에 표시되며, 각 공지의 제목, 작성일, 조회수, 댓글 수를 보여줍니다. |

### NoticeDetailActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.NoticeDetailActivity` |
| **용도** | 공지사항 상세 화면 |
| **설명** | 공지사항의 상세 내용을 표시합니다. 제목, 내용, 작성자, 작성일을 보여주고 댓글 목록과 댓글 작성 기능을 제공합니다. 조회수가 자동으로 증가합니다. |

### NoticeWriteActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.NoticeWriteActivity` |
| **용도** | 공지사항 작성/수정 화면 |
| **설명** | 동아리 관리자가 공지사항을 작성하거나 수정하는 화면입니다. 제목과 내용을 입력하고 상단 고정 여부를 설정할 수 있습니다. |

### NotificationListActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.NotificationListActivity` |
| **용도** | 알림 목록 화면 |
| **설명** | 동아리 관련 알림 목록을 표시합니다. 가입 승인, 공지사항 등록 등 다양한 알림을 읽음/미읽음 상태로 구분하여 보여줍니다. |

---

## 6. Q&A 및 채팅

### QnAActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.QnAActivity` |
| **용도** | Q&A 및 FAQ 목록 화면 |
| **설명** | 동아리 Q&A와 FAQ를 통합 관리하는 화면입니다. 탭을 통해 모두/Q&A/FAQ를 필터링할 수 있으며, 비밀 질문 기능을 지원합니다. |

### QnADetailActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.QnADetailActivity` |
| **용도** | Q&A 상세 화면 |
| **설명** | Q&A 항목의 상세 내용을 표시합니다. 질문과 답변을 보여주고, 댓글을 통한 추가 질의가 가능합니다. |

### AskQuestionActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.AskQuestionActivity` |
| **용도** | 질문 작성 화면 |
| **설명** | 새로운 Q&A 질문을 작성하는 화면입니다. 질문 내용을 입력하고 비공개 옵션을 선택할 수 있습니다. |

### ChatListActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.ChatListActivity` |
| **용도** | 채팅 상대 목록 화면 |
| **설명** | 가입한 동아리별로 부원 목록을 표시하는 화면입니다. 부원을 선택하면 해당 부원과의 채팅방이 생성됩니다. 각 부원의 직급(회장, 부회장, 총무, 회계, 부원)이 함께 표시됩니다. |

### ChatActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.ChatActivity` |
| **용도** | 채팅방 목록 화면 |
| **설명** | 현재 사용자의 모든 채팅방 목록을 카드 형태로 표시합니다. 각 채팅방의 상대방 이름, 동아리명, 마지막 메시지, 시간, 읽지 않은 메시지 수를 보여줍니다. |

### ChatDetailActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.ChatDetailActivity` |
| **용도** | 채팅 상세 화면 |
| **설명** | 실제 채팅이 이루어지는 화면입니다. 실시간 메시지 송수신, 메시지 시간 표시, 읽음 상태 확인 등의 기능을 제공합니다. |

---

## 7. 공금 관리

### BudgetHistoryActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.BudgetHistoryActivity` |
| **용도** | 공금 내역 화면 |
| **설명** | 동아리 공금 거래 내역을 조회하는 화면입니다. 지출, 수입, 조정 내역을 분류하여 표시하고, 거래별 금액, 설명, 영수증 이미지를 확인할 수 있습니다. 동아리 관리자는 새 거래를 추가할 수 있습니다. |

---

## 8. 서명 및 문서

### SignaturePadActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.SignaturePadActivity` |
| **용도** | 서명 패드 화면 |
| **설명** | 터치 입력으로 서명을 작성하는 화면입니다. 화면에 직접 손가락이나 스타일러스로 서명하고, 완료된 서명을 이미지로 저장합니다. |

### SignatureUploadActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.SignatureUploadActivity` |
| **용도** | 서명 이미지 업로드 화면 |
| **설명** | 기존 서명 이미지를 업로드하는 화면입니다. 갤러리에서 이미지를 선택하거나 카메라로 촬영하여 서명으로 등록할 수 있습니다. |

### SignatureActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.SignatureActivity` |
| **용도** | 서명 캡처 화면 |
| **설명** | 서명을 캡처하고 저장하는 화면입니다. 서명 패드와 이미지 업로드 방식을 선택할 수 있습니다. |

### DocumentGenerateActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.DocumentGenerateActivity` |
| **용도** | 문서 생성 화면 |
| **설명** | 동아리 관련 문서(가입 신청서, 중앙동아리 신청서 등)를 생성하는 화면입니다. 필요한 정보를 입력하고 서명을 추가하여 문서를 완성합니다. |

### PdfGenerationActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.PdfGenerationActivity` |
| **용도** | PDF 생성 화면 |
| **설명** | 동아리 신청서 등을 PDF 형식으로 생성하는 화면입니다. 생성된 PDF는 저장하거나 공유할 수 있습니다. |

---

## 9. 설정

### SettingsActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.SettingsActivity` |
| **용도** | 사용자 설정 화면 |
| **설명** | 사용자 정보 및 앱 설정을 관리하는 화면입니다. 프로필 정보 확인, 테마 설정(원본/흑백/그레이스케일), 알림 설정, 로그아웃 기능을 제공합니다. 최고 관리자인 경우 관리자 기능 섹션이 추가로 표시됩니다. |

### SuperAdminSettingsActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.activities.SuperAdminSettingsActivity` |
| **용도** | 최고 관리자 설정 화면 |
| **설명** | 최고 관리자 전용 설정 화면입니다. 중앙동아리 유지 최소 인원(기본 15명), 중앙동아리 등록 최소 인원(기본 20명) 등의 시스템 설정을 변경할 수 있습니다. |

---

## 10. 기타

### BaseActivity
| 항목 | 내용 |
|------|------|
| **파일 경로** | `com.example.clubmanagement.BaseActivity` |
| **용도** | 기본 Activity 클래스 |
| **설명** | 모든 Activity의 부모 클래스입니다. 테마 적용, 공통 초기화 등 모든 화면에서 공유하는 기능을 제공합니다. 실제 화면은 없으며 다른 Activity들이 이 클래스를 상속합니다. |

---

## Activity 분류 요약

### 인증 (5개)
- LoginActivity, SignUpActivity, AdminLoginActivity, AdminSignUpActivity, LoginSettingsActivity

### 메인 화면 (3개)
- MainActivityNew, AdminMainActivity, MainActivity

### 동아리 관리 (15개)
- ClubMainActivity, ClubListActivity, ClubRecommendActivity, DetailActivity
- ClubInfoActivity, ClubInfoEditActivity, ClubDescriptionEditActivity, ClubSettingsActivity
- ClubEstablishActivity, ClubApprovalListActivity, ClubDeleteActivity, MyClubsActivity
- CentralApplicationsActivity, CentralClubApplicationsActivity, DemoteCentralClubActivity
- CarouselEditActivity, EditRequestsActivity

### 부원 관리 (3개)
- MemberManagementActivity, MemberListActivity, MemberRegistrationActivity

### 공지사항 (4개)
- NoticeListActivity, NoticeDetailActivity, NoticeWriteActivity, NotificationListActivity

### Q&A 및 채팅 (5개)
- QnAActivity, QnADetailActivity, AskQuestionActivity
- ChatListActivity, 
- , ChatDetailActivity

### 공금 관리 (1개)
- BudgetHistoryActivity

### 서명 및 문서 (4개)
- SignaturePadActivity, SignatureUploadActivity, SignatureActivity
- DocumentGenerateActivity, PdfGenerationActivity

### 설정 (2개)
- SettingsActivity, SuperAdminSettingsActivity

### 기타 (1개)
- BaseActivity

---

## 화면 흐름도

```
LoginActivity (앱 시작)
    ├── SignUpActivity (회원가입)
    ├── LoginSettingsActivity
    │   ├── AdminLoginActivity → AdminMainActivity
    │   └── AdminSignUpActivity
    │
    └── MainActivityNew (로그인 성공)
        ├── [홈] ClubMainActivity (중앙동아리 가입 시)
        │   ├── NoticeListActivity → NoticeDetailActivity
        │   ├── NoticeWriteActivity
        │   ├── BudgetHistoryActivity
        │   ├── MemberManagementActivity
        │   ├── MemberListActivity
        │   └── ClubSettingsActivity
        │
        ├── [채팅] ChatListActivity → ChatActivity → ChatDetailActivity
        │
        ├── [일반동아리] ClubListActivity
        │   └── DetailActivity
        │       ├── MemberRegistrationActivity
        │       ├── QnAActivity → QnADetailActivity
        │       └── AskQuestionActivity
        │
        ├── [추천] ClubRecommendActivity
        │
        └── [내정보] SettingsActivity
            ├── MyClubsActivity
            └── SuperAdminSettingsActivity (최고관리자)
                ├── ClubApprovalListActivity
                ├── ClubDeleteActivity
                ├── CentralClubApplicationsActivity
                └── DemoteCentralClubActivity
```

---

> **총 Activity 수:** 47개
> **패키지 구조:**
> - `com.example.clubmanagement` (루트): 12개
> - `com.example.clubmanagement.activities`: 35개
