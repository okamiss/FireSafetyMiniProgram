package com.firesafety.platform.training.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firesafety.platform.training.TrainingAnswerDetail;
import com.firesafety.platform.training.TrainingAnswerDetailRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTrainingAnswerDetailRepository implements TrainingAnswerDetailRepository {
    private final TrainingAnswerDetailJpaRepository jpa;
    private final ObjectMapper objectMapper;

    public JpaTrainingAnswerDetailRepository(
            TrainingAnswerDetailJpaRepository jpa, ObjectMapper objectMapper) {
        this.jpa = jpa;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<TrainingAnswerDetail> saveAll(List<TrainingAnswerDetail> values) {
        var entities = new ArrayList<TrainingAnswerDetailEntity>();
        for (var value : values) {
            var entity = new TrainingAnswerDetailEntity();
            entity.recordId = value.recordId();
            entity.questionId = value.questionId();
            entity.userAnswerJson = writeJson(value.userAnswers());
            entity.correct = value.correct();
            entity.awardedScore = value.awardedScore();
            entities.add(entity);
        }
        return jpa.saveAll(entities).stream().map(this::toDomain).toList();
    }

    @Override public List<TrainingAnswerDetail> findByRecordId(Long recordId) {
        return jpa.findAllByRecordIdOrderByIdAsc(recordId).stream().map(this::toDomain).toList();
    }

    private TrainingAnswerDetail toDomain(TrainingAnswerDetailEntity entity) {
        return new TrainingAnswerDetail(entity.id, entity.recordId, entity.questionId,
                readJson(entity.userAnswerJson), entity.correct, entity.awardedScore);
    }

    private String writeJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Unable to serialize answer", exception); }
    }

    private Set<String> readJson(String value) {
        try { return objectMapper.readValue(value, new TypeReference<Set<String>>() {}); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Unable to read answer", exception); }
    }
}
