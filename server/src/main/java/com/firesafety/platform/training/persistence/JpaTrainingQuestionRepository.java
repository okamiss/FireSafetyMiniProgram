package com.firesafety.platform.training.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firesafety.platform.training.QuestionType;
import com.firesafety.platform.training.TrainingQuestion;
import com.firesafety.platform.training.TrainingQuestionRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTrainingQuestionRepository implements TrainingQuestionRepository {
    private final TrainingQuestionJpaRepository jpa;
    private final ObjectMapper objectMapper;

    public JpaTrainingQuestionRepository(TrainingQuestionJpaRepository jpa, ObjectMapper objectMapper) {
        this.jpa = jpa;
        this.objectMapper = objectMapper;
    }

    @Override
    public TrainingQuestion save(TrainingQuestion value) {
        var entity = value.id() == null ? new TrainingQuestionEntity() : jpa.findById(value.id()).orElseThrow();
        entity.type = value.type().name();
        entity.title = value.title();
        entity.optionsJson = writeJson(value.options());
        entity.answerJson = writeJson(value.correctAnswers());
        entity.score = value.score();
        entity.category = value.category();
        entity.explanation = value.explanation();
        entity.enabled = value.enabled();
        if (entity.createdAt == null) entity.createdAt = Instant.now();
        entity.updatedAt = Instant.now();
        return toDomain(jpa.save(entity));
    }

    @Override public List<TrainingQuestion> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override public List<TrainingQuestion> findAllById(Set<Long> ids) {
        return jpa.findAllById(ids).stream().map(this::toDomain).toList();
    }

    private TrainingQuestion toDomain(TrainingQuestionEntity entity) {
        return TrainingQuestion.restore(entity.id, QuestionType.valueOf(entity.type), entity.title,
                readJson(entity.optionsJson, new TypeReference<LinkedHashMap<String, String>>() {}),
                readJson(entity.answerJson, new TypeReference<Set<String>>() {}), entity.score,
                entity.category, entity.explanation, entity.enabled);
    }

    private String writeJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Unable to serialize question", exception); }
    }

    private <T> T readJson(String value, TypeReference<T> type) {
        try { return objectMapper.readValue(value, type); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Unable to read question", exception); }
    }
}
