package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

/**
 * Q&A 및 FAQ 항목 모델
 * type 필드로 "qna" 또는 "faq"를 구분
 */
public class QnAItem {
    private String id;
    private String clubId;          // 동아리 ID
    private String type;            // "qna" or "faq"
    private String question;        // 질문
    private String answer;          // 답변
    private String askerName;       // 질문자 이름 (Q&A only)
    private String askerId;         // 질문자 ID (Q&A only)
    private String answererName;    // 답변자 이름
    private String answererId;      // 답변자 ID
    private boolean isAnswered;     // 답변 완료 여부
    private boolean isPrivate;      // 비밀 질문 여부 (Q&A only)
    private Timestamp createdAt;    // 생성 시간
    private Timestamp answeredAt;   // 답변 시간
    private int position;           // 표시 순서 (FAQ용)

    // Firebase requires no-argument constructor
    public QnAItem() {
    }

    // Q&A 생성자
    public QnAItem(String id, String clubId, String question, String askerName, String askerId) {
        this.id = id;
        this.clubId = clubId;
        this.type = "qna";
        this.question = question;
        this.askerName = askerName;
        this.askerId = askerId;
        this.isAnswered = false;
        this.createdAt = Timestamp.now();
    }

    // FAQ 생성자
    public QnAItem(String id, String clubId, String question, String answer, int position) {
        this.id = id;
        this.clubId = clubId;
        this.type = "faq";
        this.question = question;
        this.answer = answer;
        this.isAnswered = true;
        this.position = position;
        this.createdAt = Timestamp.now();
        this.answeredAt = Timestamp.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getClubId() {
        return clubId;
    }

    public String getType() {
        return type;
    }

    public boolean isQnA() {
        return "qna".equals(type);
    }

    public boolean isFAQ() {
        return "faq".equals(type);
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public String getAskerName() {
        return askerName;
    }

    public String getAskerId() {
        return askerId;
    }

    public String getAnswererName() {
        return answererName;
    }

    public String getAnswererId() {
        return answererId;
    }

    public boolean isAnswered() {
        return isAnswered;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getAnsweredAt() {
        return answeredAt;
    }

    public int getPosition() {
        return position;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setClubId(String clubId) {
        this.clubId = clubId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public void setAskerName(String askerName) {
        this.askerName = askerName;
    }

    public void setAskerId(String askerId) {
        this.askerId = askerId;
    }

    public void setAnswererName(String answererName) {
        this.answererName = answererName;
    }

    public void setAnswererId(String answererId) {
        this.answererId = answererId;
    }

    public void setAnswered(boolean answered) {
        isAnswered = answered;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setAnsweredAt(Timestamp answeredAt) {
        this.answeredAt = answeredAt;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
