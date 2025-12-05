# 📊 동아리 추천 알고리즘 N-S 차트

> **프로젝트**: Club Management System - 동아리 추천 시스템
> **분석 대상**: ClubRecommendActivity & Club.calculateRecommendScore()
> **작성일**: 2025년 12월 3일

---

## 1. 알고리즘 개요

### 1.1 추천 시스템 워크플로우

```
[사용자 입력]
    ↓
[체크박스 선택]
- 기독교: Yes/No
- 분위기: lively / quiet
- 활동 유형: volunteer, sports, outdoor (최대 2개)
- 목적: career, academic, art (최대 2개)
    ↓
[Firebase에서 모든 동아리 조회] ← O(n)
    ↓
[각 동아리 점수 계산] ← O(n × k) = O(n)
    ↓
[점수 기준 정렬 (버블 정렬)] ← O(n²) ⚠️ 병목
    ↓
[결과 표시]
```

### 1.2 핵심 알고리즘 코드 위치

| 구성 요소 | 파일 | 메서드 | 시간 복잡도 |
|----------|------|--------|------------|
| 점수 계산 | Club.java | `calculateRecommendScore()` | O(k) ≈ O(1) |
| 정렬 | ClubRecommendActivity.java | `sortByScore()` | O(n²) |
| 전체 프로세스 | ClubRecommendActivity.java | `performRecommendation()` | O(n²) |

---

## 2. N-S 차트 (시간-공간 복잡도)

### 2.1 현재 알고리즘 성능

#### 점수 계산 알고리즘
```java
// Club.java: calculateRecommendScore()
public int calculateRecommendScore(boolean wantChristian, String wantAtmosphere,
                                   List<String> wantActivityTypes, List<String> wantPurposes) {
    int score = 0;

    // 기독교 매칭: O(1)
    if (wantChristian && isChristian) score += 30;

    // 분위기 매칭: O(1)
    if (wantAtmosphere != null && wantAtmosphere.equals(atmosphere)) score += 20;

    // 활동 유형 매칭: O(k₁) where k₁ ≤ 2
    for (String type : wantActivityTypes) {
        if (activityTypes.contains(type)) score += 15;
    }

    // 목적 매칭: O(k₂) where k₂ ≤ 2
    for (String purpose : wantPurposes) {
        if (purposes.contains(purpose)) score += 15;
    }

    return score;
}
```

**복잡도 분석:**
- **시간 복잡도**: O(k) where k = k₁ + k₂ ≤ 4
- **실제**: O(1) - 상수 시간 (최대 4회 반복)
- **공간 복잡도**: O(1) - 추가 메모리 사용 없음

#### 정렬 알고리즘 (버블 정렬)
```java
// ClubRecommendActivity.java: sortByScore()
private void sortByScore(List<Club> clubs, List<Integer> scores) {
    for (int i = 0; i < scores.size() - 1; i++) {              // n-1회
        for (int j = 0; j < scores.size() - i - 1; j++) {      // n-i-1회
            if (scores.get(j) < scores.get(j + 1)) {
                // 점수 교환
                int tempScore = scores.get(j);
                scores.set(j, scores.get(j + 1));
                scores.set(j + 1, tempScore);

                // 동아리 교환
                Club tempClub = clubs.get(j);
                clubs.set(j, clubs.get(j + 1));
                clubs.set(j + 1, tempClub);
            }
        }
    }
}
```

**복잡도 분석:**
- **비교 횟수**: (n-1) + (n-2) + ... + 1 = n(n-1)/2
- **시간 복잡도**: O(n²)
- **공간 복잡도**: O(1) - 제자리 정렬

---

## 3. 동아리 수(N)에 따른 성능 분석

### 3.1 이론적 연산 횟수

| 동아리 수 (n) | 점수 계산<br>O(n) | 정렬 (버블)<br>O(n²) | 전체 연산 | 예상 시간<br>(1연산=1μs) |
|-------------|-----------------|-------------------|----------|----------------------|
| 5           | 5               | 10                | 15       | 15μs (0.015ms) ✅     |
| 10          | 10              | 45                | 55       | 55μs (0.055ms) ✅     |
| 20          | 20              | 190               | 210      | 210μs (0.21ms) ✅     |
| 30          | 30              | 435               | 465      | 465μs (0.47ms) ✅     |
| 50          | 50              | 1,225             | 1,275    | 1.3ms ✅              |
| 100         | 100             | 4,950             | 5,050    | 5.1ms ✅              |
| 200         | 200             | 19,900            | 20,100   | 20.1ms ⚠️            |
| 500         | 500             | 124,750           | 125,250  | 125.3ms ⚠️           |
| 1,000       | 1,000           | 499,500           | 500,500  | 500.5ms ❌           |

### 3.2 시각화 - N-S 차트 (시간 복잡도)

#### 📈 실행 시간 그래프 (동아리 수 vs 실행 시간)

```
실행 시간 (ms)
     │
 600 │                                                                    ●  O(n²) 버블 정렬
     │                                                                 ╱
 500 │                                                              ●
     │                                                           ╱
 400 │                                                        ╱
     │                                                     ╱
 300 │                                                  ╱
     │                                               ╱
 200 │                                            ●
     │                                         ╱
 150 │                                      ╱
     │                                   ╱
 100 │                                ●
     │                             ╱
  50 │                          ╱
     │                      ●╱
  20 │                   ●●
  10 │                ●●
   5 │             ●●●
   2 │          ●●●
   1 │       ●●●
 0.5│    ●●●
 0.1│ ●●●───────────────────────────────────────────────────────────── O(n) 점수 계산
   0└───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴────→ 동아리 수 (n)
      5  10  20  30  50  75 100 125 150 200 250 300 400 500 1000
```

#### 📊 N-S 차트 박스 다이어그램

```
┌─────────────────────────────────────────────────────────────────────┐
│                      동아리 추천 알고리즘 N-S 차트                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  [입력] n = 동아리 수                                                 │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  1. Firebase 조회        T(n) = O(n)   S(n) = O(n)      │      │
│  │     ⏱ 실행시간: ~50ms (네트워크 의존)                       │      │
│  └──────────────────────────────────────────────────────────┘      │
│                            ↓                                        │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  2. 점수 계산 (n개)      T(n) = O(n)   S(n) = O(n)      │      │
│  │     각 동아리당: O(1) - 상수 시간                          │      │
│  │     전체: n × O(1) = O(n)                                │      │
│  │     ⏱ 실행시간 (n=500): ~5ms                            │      │
│  └──────────────────────────────────────────────────────────┘      │
│                            ↓                                        │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  3. 정렬 (버블 정렬)     T(n) = O(n²)  S(n) = O(1)  ⚠️  │      │
│  │     비교 횟수: n(n-1)/2                                  │      │
│  │     교환 횟수: 최대 n(n-1)/2                             │      │
│  │     ⏱ 실행시간 (n=500): ~125ms  ← 🔴 병목!              │      │
│  └──────────────────────────────────────────────────────────┘      │
│                            ↓                                        │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  4. 결과 표시           T(n) = O(n)   S(n) = O(1)       │      │
│  │     ⏱ 실행시간: ~1ms                                     │      │
│  └──────────────────────────────────────────────────────────┘      │
│                                                                     │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━        │
│  전체 시간 복잡도:  T(n) = O(n²)                                     │
│  전체 공간 복잡도:  S(n) = O(n)                                      │
│  실행 시간 (n=500): ~181ms                                          │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━        │
└─────────────────────────────────────────────────────────────────────┘
```

#### 🎯 성능 평가 기준

```
┌───────────────┬──────────────┬────────────┬──────────────────┐
│  동아리 수     │  실행 시간    │  사용자 체감 │      평가        │
├───────────────┼──────────────┼────────────┼──────────────────┤
│  n ≤ 50       │  < 2ms       │  즉시      │  ✅ 우수         │
│  50 < n ≤ 100 │  2-10ms      │  즉시      │  ✅ 좋음         │
│  100 < n ≤ 200│  10-20ms     │  빠름      │  ⚠️  보통        │
│  200 < n ≤ 500│  20-130ms    │  약간 느림  │  ⚠️  개선 필요   │
│  n > 500      │  > 130ms     │  느림      │  ❌ 즉시 개선    │
└───────────────┴──────────────┴────────────┴──────────────────┘
```

**해석:**
- **n ≤ 100**: 사용자가 체감할 수 없는 수준 (< 10ms) ✅
- **100 < n ≤ 200**: 약간 느려질 수 있음 (10-20ms) ⚠️
- **n > 200**: 명확하게 느려짐 (> 20ms) ❌
- **병목 지점**: 정렬 알고리즘이 전체 실행 시간의 **약 70%** 차지

---

## 4. 공간 복잡도 분석

### 4.1 메모리 사용량

| 데이터 구조 | 크기 | 공간 복잡도 | 설명 |
|-----------|------|-----------|------|
| clubs (List<Club>) | n | O(n) | 동아리 객체 리스트 |
| scores (List<Integer>) | n | O(n) | 점수 리스트 |
| Firebase 데이터 | n | O(n) | 조회된 동아리 데이터 |
| 정렬 임시 변수 (tempScore, tempClub) | 1 | O(1) | 버블 정렬 교환용 |
| **총 공간 복잡도** | - | **O(n)** | - |

### 4.2 메모리 사용량 예측

| 동아리 수 (n) | Club 객체<br>(각 2KB) | scores<br>(각 4B) | 총 메모리 |
|-------------|---------------------|------------------|----------|
| 10          | 20 KB               | 40 B             | ~20 KB   |
| 50          | 100 KB              | 200 B            | ~100 KB  |
| 100         | 200 KB              | 400 B            | ~200 KB  |
| 500         | 1 MB                | 2 KB             | ~1 MB    |
| 1,000       | 2 MB                | 4 KB             | ~2 MB    |

**결론:** 메모리는 문제없음 (모바일 환경에서도 충분)

---

## 5. 병목 지점 분석

### 5.1 프로파일링 결과 (예상)

```
전체 실행 시간: 125ms (동아리 500개 기준)

Firebase 조회:        50ms  ████████████████ (40%)
점수 계산:             5ms  ██ (4%)
정렬 (버블 정렬):      70ms  ██████████████████████ (56%) ← 🔴 병목!
UI 업데이트:           0ms  (무시 가능)
```

**병목 포인트:** 정렬 알고리즘이 전체 시간의 **56%** 차지

---

## 6. 최적화 권장 사항

### 6.1 즉시 적용 가능 - Collections.sort() 사용

#### Before (현재)
```java
// O(n²) 버블 정렬 - 느림
private void sortByScore(List<Club> clubs, List<Integer> scores) {
    for (int i = 0; i < scores.size() - 1; i++) {
        for (int j = 0; j < scores.size() - i - 1; j++) {
            if (scores.get(j) < scores.get(j + 1)) {
                // 교환...
            }
        }
    }
}
```

#### After (권장)
```java
// O(n log n) Timsort - 빠름
private void sortByScore(List<Club> clubs, List<Integer> scores) {
    // 1. 인덱스와 함께 정렬하기 위한 Pair 리스트 생성
    List<Pair<Club, Integer>> pairs = new ArrayList<>();
    for (int i = 0; i < clubs.size(); i++) {
        pairs.add(new Pair<>(clubs.get(i), scores.get(i)));
    }

    // 2. Collections.sort()로 정렬 (O(n log n))
    Collections.sort(pairs, (a, b) -> b.second - a.second);  // 내림차순

    // 3. 정렬된 결과를 원본 리스트에 복사
    for (int i = 0; i < pairs.size(); i++) {
        clubs.set(i, pairs.get(i).first);
        scores.set(i, pairs.get(i).second);
    }
}

// Pair 클래스 (Android에서 사용 가능)
import android.util.Pair;
```

**또는 더 간단하게:**
```java
// ScoredClub 클래스 생성
class ScoredClub {
    Club club;
    int score;

    ScoredClub(Club club, int score) {
        this.club = club;
        this.score = score;
    }
}

// 정렬
List<ScoredClub> scoredClubs = new ArrayList<>();
for (Club club : clubs) {
    int score = club.calculateRecommendScore(...);
    scoredClubs.add(new ScoredClub(club, score));
}

// 한 줄로 정렬! (O(n log n))
Collections.sort(scoredClubs, (a, b) -> b.score - a.score);
```

### 6.2 성능 개선 효과

| 동아리 수 | 버블 정렬 (현재) | Collections.sort (개선) | 개선율 |
|---------|----------------|----------------------|-------|
| 10      | 0.055ms        | 0.033ms              | 1.7x ↑ |
| 50      | 1.3ms          | 0.28ms               | 4.6x ↑ |
| 100     | 5.1ms          | 0.66ms               | 7.7x ↑ |
| 200     | 20.1ms         | 1.53ms               | 13.1x ↑ |
| 500     | 125ms          | 4.5ms                | **27.8x ↑** |
| 1,000   | 500ms          | 10ms                 | **50x ↑** |

**결론:** 동아리 500개 기준 **27.8배 빠름** (125ms → 4.5ms)

---

## 7. 개선 후 N-S 차트

### 7.1 시간 복잡도 비교 그래프

#### 📊 현재 vs 개선 성능 비교

```
실행 시간 (ms)
     │
 600 │                                                                ╱
     │                                                             ╱ │
 500 │                                                          ●    │  O(n²) 버블 정렬
     │                                                       ╱       │  (현재)
 400 │                                                    ╱          │
     │                                                 ╱             │
 300 │                                              ╱                │
     │                                           ╱                   │
 200 │                                        ●                      │
     │                                     ╱                         │
 150 │                                  ╱                            │
     │                               ╱                               │
 100 │                            ●                                  │
     │                         ╱                                     │
  50 │                      ╱                                        │
     │                   ●╱                                          │
  20 │                ●●                                             │
  10 │             ●●●────────────────────────────────────────────── │ 사용자 체감 임계값
   5 │          ●●●                 ●───────●───────●───────●───●   │ (10ms)
   2 │       ●●●                  ●                                 │
   1 │    ●●●                   ●                                   │  O(n log n)
 0.5│ ●●●                     ●                                     │  Collections.sort
 0.1│●                      ●                                       │  (개선)
   0└───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴────→ 동아리 수 (n)
      5  10  20  30  50  75 100 125 150 200 250 300 400 500 1000
```

**범례:**
- 🔴 **O(n²) 버블 정렬** (현재): 동아리 수 증가 시 기하급수적 증가
- 🟢 **O(n log n) Collections.sort** (개선): 거의 선형에 가까운 증가
- ⚠️ **사용자 체감 임계값 (10ms)**: 이 아래는 즉각 반응으로 느껴짐

### 7.2 개선 후 N-S 차트 박스 다이어그램

```
┌─────────────────────────────────────────────────────────────────────┐
│                  개선 후 동아리 추천 알고리즘 N-S 차트                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  [입력] n = 동아리 수                                                 │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  1. Firebase 조회        T(n) = O(n)   S(n) = O(n)      │      │
│  │     ⏱ 실행시간: ~50ms (네트워크 의존)                       │      │
│  └──────────────────────────────────────────────────────────┘      │
│                            ↓                                        │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  2. 점수 계산 (n개)      T(n) = O(n)   S(n) = O(n)      │      │
│  │     각 동아리당: O(1) - 상수 시간                          │      │
│  │     전체: n × O(1) = O(n)                                │      │
│  │     ⏱ 실행시간 (n=500): ~5ms                            │      │
│  └──────────────────────────────────────────────────────────┘      │
│                            ↓                                        │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  3. 정렬 (Collections.sort) T(n)=O(n log n) S(n)=O(log n)│     │
│  │     알고리즘: Timsort (합병 + 삽입 정렬 하이브리드)        │      │
│  │     비교 횟수: ~n log₂ n                                 │      │
│  │     ⏱ 실행시간 (n=500): ~4.5ms  ← ✨ 27.8배 개선!        │      │
│  └──────────────────────────────────────────────────────────┘      │
│                            ↓                                        │
│  ┌──────────────────────────────────────────────────────────┐      │
│  │  4. 결과 표시           T(n) = O(n)   S(n) = O(1)       │      │
│  │     ⏱ 실행시간: ~1ms                                     │      │
│  └──────────────────────────────────────────────────────────┘      │
│                                                                     │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━        │
│  전체 시간 복잡도:  T(n) = O(n log n)  ✨                            │
│  전체 공간 복잡도:  S(n) = O(n)                                      │
│  실행 시간 (n=500): ~60.5ms  (개선 전: 181ms)                       │
│  개선율:            3.0배 빠름! 🚀                                   │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━        │
└─────────────────────────────────────────────────────────────────────┘
```

### 7.3 전체 프로세스 성능 (개선 후)

#### 📋 단계별 성능 분석

| 단계 | 시간 복잡도 | 동아리 500개 기준 |
|-----|-----------|-----------------|
| Firebase 조회 | O(n) | 50ms |
| 점수 계산 | O(n) | 5ms |
| 정렬 (개선) | **O(n log n)** | **4.5ms** ✨ |
| UI 업데이트 | O(n) | 1ms |
| **전체** | **O(n log n)** | **60.5ms** |

**개선 효과:**
- 개선 전: 125ms → 개선 후: 60.5ms
- **2.1배 빠름** (전체 프로세스 기준)
- 정렬만 비교하면 **27.8배 빠름**

---

## 8. 구현 우선순위

### High Priority 🔴 (즉시 적용)
1. **정렬 알고리즘을 Collections.sort()로 변경**
   - 파일: `ClubRecommendActivity.java`
   - 메서드: `sortByScore()`
   - 예상 작업 시간: 10분
   - 성능 향상: **27.8배**

### Medium Priority 🟡 (추후 개선)
2. **ScoredClub 클래스 도입**
   - 코드 가독성 향상
   - 유지보수 용이

3. **Firebase 쿼리 최적화**
   - `hasKeywords == true` 필터 추가
   - 불필요한 동아리 조회 방지

### Low Priority 🟢 (선택 사항)
4. **캐싱 구현**
   ```java
   // 한 번 정렬한 결과 저장
   private List<Club> cachedResults;
   private long lastSearchTime;
   ```

5. **페이징 구현**
   - 상위 N개만 먼저 표시
   - 스크롤 시 추가 로드

---

## 9. 실제 코드 수정 예시

### 수정 전 (ClubRecommendActivity.java:248-265)
```java
private void sortByScore(List<Club> clubs, List<Integer> scores) {
    // 간단한 버블 정렬로 점수 내림차순 정렬
    for (int i = 0; i < scores.size() - 1; i++) {
        for (int j = 0; j < scores.size() - i - 1; j++) {
            if (scores.get(j) < scores.get(j + 1)) {
                // 점수 교환
                int tempScore = scores.get(j);
                scores.set(j, scores.get(j + 1));
                scores.set(j + 1, tempScore);

                // 클럽도 교환
                Club tempClub = clubs.get(j);
                clubs.set(j, clubs.get(j + 1));
                clubs.set(j + 1, tempClub);
            }
        }
    }
}
```

### 수정 후 (권장)
```java
import android.util.Pair;
import java.util.Collections;

private void sortByScore(List<Club> clubs, List<Integer> scores) {
    // Pair 리스트 생성 (Club + Score)
    List<Pair<Club, Integer>> pairs = new ArrayList<>();
    for (int i = 0; i < clubs.size(); i++) {
        pairs.add(new Pair<>(clubs.get(i), scores.get(i)));
    }

    // Collections.sort()로 정렬 - O(n log n)
    Collections.sort(pairs, new Comparator<Pair<Club, Integer>>() {
        @Override
        public int compare(Pair<Club, Integer> a, Pair<Club, Integer> b) {
            return b.second - a.second;  // 내림차순 (점수 높은 순)
        }
    });

    // 정렬된 결과를 원본 리스트에 반영
    clubs.clear();
    scores.clear();
    for (Pair<Club, Integer> pair : pairs) {
        clubs.add(pair.first);
        scores.add(pair.second);
    }
}
```

**또는 람다식 사용 (Android API 24+):**
```java
private void sortByScore(List<Club> clubs, List<Integer> scores) {
    List<Pair<Club, Integer>> pairs = new ArrayList<>();
    for (int i = 0; i < clubs.size(); i++) {
        pairs.add(new Pair<>(clubs.get(i), scores.get(i)));
    }

    // 람다식으로 간결하게
    Collections.sort(pairs, (a, b) -> b.second - a.second);

    clubs.clear();
    scores.clear();
    for (Pair<Club, Integer> pair : pairs) {
        clubs.add(pair.first);
        scores.add(pair.second);
    }
}
```

---

## 10. 성능 테스트 계획

### 10.1 테스트 케이스

| 테스트 ID | 동아리 수 | 선택 조건 | 예상 결과 | 측정 항목 |
|---------|---------|---------|----------|----------|
| T1      | 10      | 기본     | < 1ms    | 실행 시간 |
| T2      | 50      | 전체     | < 2ms    | 실행 시간 |
| T3      | 100     | 기본     | < 10ms   | 실행 시간 |
| T4      | 500     | 전체     | < 10ms   | 실행 시간, 메모리 |
| T5      | 1,000   | 기본     | < 20ms   | 실행 시간, 메모리 |

### 10.2 측정 코드
```java
private void performRecommendation() {
    // 시작 시간 측정
    long startTime = System.currentTimeMillis();

    // ... 기존 로직 ...

    firebaseManager.getAllClubs(new FirebaseManager.ClubListCallback() {
        @Override
        public void onSuccess(List<Club> clubs) {
            // 정렬 시작 시간
            long sortStartTime = System.currentTimeMillis();

            sortByScore(matchedClubs, matchedScores);

            // 정렬 종료 시간
            long sortEndTime = System.currentTimeMillis();
            long sortDuration = sortEndTime - sortStartTime;

            // 전체 종료 시간
            long endTime = System.currentTimeMillis();
            long totalDuration = endTime - startTime;

            // 로그 출력
            Log.d("Performance", "동아리 수: " + clubs.size());
            Log.d("Performance", "정렬 시간: " + sortDuration + "ms");
            Log.d("Performance", "전체 시간: " + totalDuration + "ms");
        }
    });
}
```

---

## 11. 결론

### 11.1 현재 상태 평가
- ✅ **소규모 (n ≤ 50)**: 문제 없음, 현재 알고리즘으로 충분
- ⚠️ **중규모 (50 < n ≤ 100)**: 약간 느림, 개선 권장
- ❌ **대규모 (n > 100)**: 명확히 느림, **즉시 개선 필요**

### 11.2 권장 조치
**즉시 적용:**
- `sortByScore()` 메서드를 `Collections.sort()`로 교체
- 예상 작업 시간: **10분**
- 성능 향상: **27.8배** (동아리 500개 기준)
- 실행 시간 단축: **125ms → 4.5ms**

### 11.3 기대 효과
```
개선 전:
Firebase(50ms) + 점수계산(5ms) + 정렬(70ms) = 125ms ⚠️

개선 후:
Firebase(50ms) + 점수계산(5ms) + 정렬(4.5ms) = 59.5ms ✅

→ 2.1배 빠름, 사용자 경험 대폭 개선
```

---

**작성자**: 김준성 (2010045)
**제출일**: 2025년 12월 3일
**프로젝트**: Club Management System - 동아리 추천 알고리즘 성능 분석
