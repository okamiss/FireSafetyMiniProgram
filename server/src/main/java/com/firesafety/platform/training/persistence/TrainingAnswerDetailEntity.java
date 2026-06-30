package com.firesafety.platform.training.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "training_answer_detail")
class TrainingAnswerDetailEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "record_id", nullable = false) Long recordId;
    @Column(name = "question_id", nullable = false) Long questionId;
    @Column(name = "user_answer_json", nullable = false, length = 1000) String userAnswerJson;
    @Column(nullable = false) boolean correct;
    @Column(name = "awarded_score", nullable = false) int awardedScore;

    protected TrainingAnswerDetailEntity() {}
}
