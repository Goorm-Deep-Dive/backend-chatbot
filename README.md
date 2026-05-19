# 동행 AI Chatbot

- 공식 문서와 사용자 상황을 기반으로 사망 이후 절차를 안내하는 RAG 기반 AI 챗봇 서버

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?logo=springboot&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring_AI-RAG-4285F4?logo=spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?logo=springsecurity&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)
![AWS](https://img.shields.io/badge/AWS_EC2-FF9900?logo=amazonec2&logoColor=white)

# 1. Project Overview

- 동행 AI Chatbot은 사망 이후 유족이 처리해야 하는 절차를 공식 문서 기반으로 안내하는 AI 챗봇 서버입니다.

- 공공기관 문서 검색 결과와 사용자 체크리스트 정보를 함께 반영하여 출처 기반의 개인화된 답변을 제공합니다.

---

# 2. Features

| 기능 | 설명 |
|---|---|
| RAG 기반 답변 | 공식 문서를 벡터 DB에서 검색하여 답변 근거로 사용 |
| 개인화 응답 | 사용자 체크리스트와 이전 대화 내역 반영 |
| 출처 기반 응답 | 참고한 공식 문서 Source 제공 |
| Prompt Injection 방어 | 사용자 입력과 참고문서를 명령이 아닌 데이터로 처리 |
| SSE 스트리밍 | AI 응답을 실시간으로 스트리밍 |
| Virtual Thread 적용 | 동시 스트리밍 요청 처리 안정성 개선 |

---

# 3. AI Response Pipeline

```text
사용자 질문
        ↓
공식 자료 벡터 DB 검색
(RAG / Cosine Similarity)
        ↓
사용자 Context 결합
(체크리스트 · 이전 대화 반영)
        ↓
User Prompt 구성
(XML 기반 Context 구조화)
        ↓
System Prompt 구성
(답변 정책 · Prompt Injection 방어)
        ↓
LLM 응답 생성
        ↓
출처 기반 개인화 응답 반환
```

---

# 4. RAG Pipeline

## 4.1 Document Retrieval
- 공공기관 PDF 문서 기반 벡터 검색
- Cosine Similarity 기반 유사 문서 탐색
- Top-K 기반 관련 문서 검색


## 4.2 Chunking Strategy
- Chunk Size 500 기반 문서 청킹
- 행정 · 법률 문서의 문맥 유지 고려
- Garbage Chunk Filtering 기반 불필요한 텍스트 제거


## 4.3 Source Management
- 문서 단위 Source 메타데이터 관리
- 출처 기반 응답 생성
- Reference Source 정규화 및 중복 제거

---

# 5. Prompt Engineering

## System Prompt
- 답변 범위 제한
- 답변 톤 및 출력 형식 제어
- 참고문서 기반 응답 정책
- 체크리스트 기반 개인화 응답 정책
- Prompt Injection 방어 규칙 적용


## User Prompt
XML 기반으로 Context를 구조화하여  
LLM이 각 데이터를 명확히 구분하도록 구성

### 포함 정보
- 사용자 질문
- 사용자 체크리스트 현황
- 이전 대화 내역
- RAG 검색 결과 문서
- 참고문서 Source 목록

---

# 6. Run

## Prerequisites

- PostgreSQL 실행 필요
- OpenAI 또는 Google API Key 필요

## Configuration

`application-example.yml`을 참고하여  
`src/main/resources/application.yml` 파일을 생성합니다.

## Local Run
```
./gradlew bootRun
```

## Docker Run
```
./gradlew build -x test

docker build -t chatbot-app:latest .

docker run -d \
  --name chatbot-app \
  -p 8082:8082 \
  chatbot-app:latest
```


# 7. Example

## 질문

```text
사망신고는 어떻게 하나요?
```

## 답변

```text
사망신고는 사망 사실을 알게 된 날부터  
1개월 이내에 해야 합니다.

신고는 사망지, 매장지 또는 화장지의  
시·구·읍·면 사무소나 주민센터에서 할 수 있으며,  
방문 또는 우편으로 신청 가능합니다.

신고 시에는 다음 서류가 필요합니다.
- 사망진단서 또는 검안서
- 신고인 신분증 사본
- 가족관계등록부(전산 확인 가능 시 생략 가능)

현재 체크리스트 기준으로  
사망신고 절차는 미완료 상태이며,  
2026년 6월 19일까지 처리해야 합니다.

[출처]
- 생활법령정보 사망신고
- 가족관계의 등록 등에 관한 법률
```
