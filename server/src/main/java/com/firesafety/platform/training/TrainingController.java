package com.firesafety.platform.training;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.common.ApiResponse;
import com.firesafety.platform.common.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class TrainingController {
    private static final long MAX_IMPORT_SIZE = 5 * 1024 * 1024;

    private final TrainingManagementService management;
    private final TrainingAttemptService attempts;
    private final TrainingQuestionExcelParser excelParser;

    public TrainingController(
            TrainingManagementService management,
            TrainingAttemptService attempts,
            TrainingQuestionExcelParser excelParser) {
        this.management = management;
        this.attempts = attempts;
        this.excelParser = excelParser;
    }

    @PostMapping("/api/admin/training/questions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ApiResponse<QuestionResponse> createQuestion(
            @AuthenticationPrincipal SessionPrincipal principal,
            @Valid @RequestBody QuestionRequest request) {
        return ApiResponse.ok(QuestionResponse.from(management.createQuestion(principal, request.toCommand())));
    }

    @GetMapping("/api/admin/training/questions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ApiResponse<List<QuestionResponse>> questions(@AuthenticationPrincipal SessionPrincipal principal) {
        return ApiResponse.ok(management.listQuestions(principal).stream().map(QuestionResponse::from).toList());
    }

    @GetMapping("/api/admin/training/questions/import-template")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ResponseEntity<ByteArrayResource> questionImportTemplate() {
        try {
            var bytes = excelParser.createTemplate();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(bytes.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename("培训题库导入模板.xlsx", StandardCharsets.UTF_8).build().toString())
                    .body(new ByteArrayResource(bytes));
        } catch (IOException exception) {
            throw new BusinessException("QUESTION_TEMPLATE_FAILED", "题库导入模板生成失败");
        }
    }

    @PostMapping(
            value = "/api/admin/training/questions/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ApiResponse<QuestionImportResponse> importQuestions(
            @AuthenticationPrincipal SessionPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        validateImportFile(file);
        try {
            var parsed = excelParser.parse(file.getInputStream());
            if (!parsed.errors().isEmpty()) {
                return ApiResponse.ok(new QuestionImportResponse(parsed.totalRows(), 0, parsed.errors()));
            }
            var imported = management.importQuestions(principal, parsed.questions());
            return ApiResponse.ok(new QuestionImportResponse(parsed.totalRows(), imported.size(), List.of()));
        } catch (IOException | IllegalArgumentException exception) {
            throw new BusinessException("INVALID_QUESTION_IMPORT_FILE", "Excel 文件无法读取或格式不正确");
        }
    }

    private void validateImportFile(MultipartFile file) {
        var filename = file.getOriginalFilename();
        if (file.isEmpty() || file.getSize() > MAX_IMPORT_SIZE) {
            throw new BusinessException("INVALID_IMPORT_FILE_SIZE", "Excel 文件不能为空且不能超过 5MB");
        }
        if (filename == null || !filename.toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")) {
            throw new BusinessException("INVALID_IMPORT_FILE_TYPE", "仅支持 .xlsx 格式的题库文件");
        }
    }

    @PostMapping("/api/admin/training/tasks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ApiResponse<TaskResponse> createTask(
            @AuthenticationPrincipal SessionPrincipal principal,
            @Valid @RequestBody TaskRequest request) {
        return ApiResponse.ok(TaskResponse.from(management.createTask(principal, request.toCommand())));
    }

    @GetMapping("/api/admin/training/tasks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ApiResponse<List<TaskResponse>> tasks(@AuthenticationPrincipal SessionPrincipal principal) {
        return ApiResponse.ok(management.listTasks(principal).stream().map(TaskResponse::from).toList());
    }

    @PostMapping("/api/admin/training/tasks/{id}/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_OPERATOR')")
    public ApiResponse<TaskResponse> publish(
            @AuthenticationPrincipal SessionPrincipal principal, @PathVariable Long id) {
        return ApiResponse.ok(TaskResponse.from(management.publish(principal, id)));
    }

    @GetMapping("/api/miniapp/training/tasks")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ENTERPRISE_ADMIN')")
    public ApiResponse<List<AssignedTaskResponse>> assignedTasks(
            @AuthenticationPrincipal SessionPrincipal principal) {
        return ApiResponse.ok(attempts.assignments(principal).stream().map(AssignedTaskResponse::from).toList());
    }

    @GetMapping("/api/miniapp/training/tasks/{id}/paper")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ENTERPRISE_ADMIN')")
    public ApiResponse<PaperResponse> paper(
            @AuthenticationPrincipal SessionPrincipal principal, @PathVariable Long id) {
        return ApiResponse.ok(PaperResponse.from(attempts.paper(principal, id)));
    }

    @PostMapping("/api/miniapp/training/tasks/{id}/submit")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ENTERPRISE_ADMIN')")
    public ApiResponse<AttemptResponse> submit(
            @AuthenticationPrincipal SessionPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody SubmitRequest request) {
        return ApiResponse.ok(AttemptResponse.from(attempts.submit(principal, id, request.answers())));
    }

    public record QuestionRequest(
            @NotNull QuestionType type,
            @NotBlank @Size(max = 1000) String title,
            @NotEmpty Map<String, String> options,
            @NotEmpty Set<String> correctAnswers,
            @Min(1) @Max(1000) int score,
            @NotBlank @Size(max = 100) String category,
            @Size(max = 2000) String explanation) {
        CreateTrainingQuestionCommand toCommand() {
            return new CreateTrainingQuestionCommand(
                    type, title, options, correctAnswers, score, category, explanation);
        }
    }

    public record TaskRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @NotNull Instant startAt,
            @NotNull Instant endAt,
            @Min(1) @Max(1000) int passScore,
            @Min(1) @Max(10) int maxAttempts,
            @NotEmpty Set<Long> questionIds,
            Set<Long> targetEnterpriseIds,
            Set<Long> targetUserIds) {
        CreateTrainingTaskCommand toCommand() {
            return new CreateTrainingTaskCommand(title, description, startAt, endAt, passScore, maxAttempts,
                    questionIds, targetEnterpriseIds == null ? Set.of() : targetEnterpriseIds,
                    targetUserIds == null ? Set.of() : targetUserIds);
        }
    }

    public record SubmitRequest(@NotEmpty Map<Long, Set<String>> answers) {}

    public record QuestionImportResponse(
            int totalRows, int importedRows, List<QuestionImportError> errors) {}

    public record QuestionResponse(
            Long id, QuestionType type, String title, Map<String, String> options,
            Set<String> correctAnswers, int score, String category, String explanation, boolean enabled) {
        static QuestionResponse from(TrainingQuestion value) {
            return new QuestionResponse(value.id(), value.type(), value.title(), value.options(),
                    value.correctAnswers(), value.score(), value.category(), value.explanation(), value.enabled());
        }
    }

    public record TaskResponse(
            Long id, String title, String description, Instant startAt, Instant endAt,
            int passScore, int maxAttempts, TrainingTaskStatus status, Set<Long> questionIds,
            Set<Long> targetEnterpriseIds, Set<Long> targetUserIds, Instant publishedAt) {
        static TaskResponse from(TrainingTask value) {
            return new TaskResponse(value.id(), value.title(), value.description(), value.startAt(), value.endAt(),
                    value.passScore(), value.maxAttempts(), value.status(), value.questionIds(),
                    value.targetEnterpriseIds(), value.targetUserIds(), value.publishedAt());
        }
    }

    public record AssignedTaskResponse(
            TaskResponse task, int attemptsUsed, int bestScore, boolean passed, Instant completedAt) {
        static AssignedTaskResponse from(AssignedTrainingTask value) {
            return new AssignedTaskResponse(TaskResponse.from(value.task()), value.participant().attemptsUsed(),
                    value.participant().bestScore(), value.participant().passed(), value.participant().completedAt());
        }
    }

    public record PaperQuestionResponse(
            Long id, QuestionType type, String title, Map<String, String> options, int score, String category) {
        static PaperQuestionResponse from(TrainingQuestion value) {
            return new PaperQuestionResponse(
                    value.id(), value.type(), value.title(), value.options(), value.score(), value.category());
        }
    }

    public record PaperResponse(
            TaskResponse task, int attemptsUsed, List<PaperQuestionResponse> questions) {
        static PaperResponse from(TrainingPaper value) {
            return new PaperResponse(TaskResponse.from(value.task()), value.participant().attemptsUsed(),
                    value.questions().stream().map(PaperQuestionResponse::from).toList());
        }
    }

    public record AttemptDetailResponse(
            Long questionId, Set<String> userAnswers, Set<String> correctAnswers,
            boolean correct, int awardedScore, String explanation) {}

    public record AttemptResponse(
            Long recordId, int score, boolean passed, int attemptNo, Instant submittedAt,
            List<AttemptDetailResponse> details) {
        static AttemptResponse from(TrainingAttemptResult value) {
            var questionById = value.questions().stream()
                    .collect(Collectors.toMap(TrainingQuestion::id, Function.identity()));
            var details = value.details().stream().map(detail -> {
                var question = questionById.get(detail.questionId());
                return new AttemptDetailResponse(detail.questionId(), detail.userAnswers(),
                        question == null ? Set.of() : question.correctAnswers(), detail.correct(),
                        detail.awardedScore(), question == null ? null : question.explanation());
            }).toList();
            return new AttemptResponse(value.record().id(), value.record().score(), value.record().passed(),
                    value.record().attemptNo(), value.record().submittedAt(), details);
        }
    }
}
