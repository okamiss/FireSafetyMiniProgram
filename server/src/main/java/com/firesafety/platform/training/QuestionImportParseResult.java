package com.firesafety.platform.training;

import java.util.List;

public record QuestionImportParseResult(
        int totalRows,
        List<CreateTrainingQuestionCommand> questions,
        List<QuestionImportError> errors) {}
