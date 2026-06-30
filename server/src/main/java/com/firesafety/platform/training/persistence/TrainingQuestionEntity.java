package com.firesafety.platform.training.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "training_question")
class TrainingQuestionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false, length = 32) String type;
    @Column(nullable = false, length = 1000) String title;
    @Column(name = "options_json", nullable = false, length = 4000) String optionsJson;
    @Column(name = "answer_json", nullable = false, length = 1000) String answerJson;
    @Column(nullable = false) int score;
    @Column(length = 100) String category;
    @Column(length = 2000) String explanation;
    @Column(nullable = false) boolean enabled;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;

    protected TrainingQuestionEntity() {}
}
