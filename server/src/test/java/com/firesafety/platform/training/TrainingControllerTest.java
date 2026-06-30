package com.firesafety.platform.training;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.GlobalExceptionHandler;
import java.time.Instant;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TrainingControllerTest {
    @Mock private TrainingManagementService management;
    @Mock private TrainingAttemptService attempts;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new TrainingController(management, attempts, new TrainingQuestionExcelParser()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(
                        new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
                                new ObjectMapper().findAndRegisterModules()),
                        new org.springframework.http.converter.ResourceHttpMessageConverter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void operatorCreatesQuestion() throws Exception {
        var principal = new SessionPrincipal(1L, UserRole.PLATFORM_OPERATOR, null, "运营");
        authenticate(principal);
        when(management.createQuestion(any(), any())).thenReturn(question());

        mockMvc.perform(post("/api/admin/training/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"SINGLE_CHOICE","title":"灭火器如何使用？",
                                 "options":{"A":"拔掉保险销","B":"乘坐电梯"},
                                 "correctAnswers":["A"],"score":100,"category":"消防常识","explanation":"先拔保险销"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.correctAnswers[0]").value("A"));
    }

    @Test
    void paperDoesNotExposeCorrectAnswersBeforeSubmission() throws Exception {
        var principal = new SessionPrincipal(20L, UserRole.EMPLOYEE, 30L, "张三");
        authenticate(principal);
        var participant = TrainingParticipant.assigned(10L, 20L, 30L);
        when(attempts.paper(principal, 10L)).thenReturn(new TrainingPaper(task(), participant, List.of(question())));

        mockMvc.perform(get("/api/miniapp/training/tasks/10/paper"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions[0].title").value("灭火器如何使用？"))
                .andExpect(jsonPath("$.data.questions[0].correctAnswers").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].explanation").doesNotExist());
    }

    @Test
    void operatorImportsValidatedXlsxQuestions() throws Exception {
        var principal = new SessionPrincipal(1L, UserRole.PLATFORM_OPERATOR, null, "运营");
        authenticate(principal);
        when(management.importQuestions(any(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var commands = (List<CreateTrainingQuestionCommand>) invocation.getArgument(1);
            return commands.stream().map(command -> TrainingQuestion.restore(
                    201L, command.type(), command.title(), command.options(), command.correctAnswers(),
                    command.score(), command.category(), command.explanation(), true)).toList();
        });
        var file = new MockMultipartFile(
                "file", "questions.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                questionWorkbook());

        mockMvc.perform(multipart("/api/admin/training/questions/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRows").value(1))
                .andExpect(jsonPath("$.data.importedRows").value(1))
                .andExpect(jsonPath("$.data.errors").isEmpty());
    }

    @Test
    void operatorDownloadsQuestionImportTemplate() throws Exception {
        authenticate(new SessionPrincipal(1L, UserRole.PLATFORM_OPERATOR, null, "运营"));

        mockMvc.perform(get("/api/admin/training/questions/import-template"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")));
    }

    private void authenticate(SessionPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "access"));
    }

    private TrainingQuestion question() {
        var options = new LinkedHashMap<String, String>();
        options.put("A", "拔掉保险销");
        options.put("B", "乘坐电梯");
        return TrainingQuestion.restore(101L, QuestionType.SINGLE_CHOICE, "灭火器如何使用？",
                options, Set.of("A"), 100, "消防常识", "先拔保险销", true);
    }

    private TrainingTask task() {
        return TrainingTask.restore(10L, "消防基础培训", "完成答题",
                Instant.parse("2026-06-01T00:00:00Z"), Instant.parse("2026-07-31T00:00:00Z"),
                60, 3, TrainingTaskStatus.PUBLISHED, Set.of(101L), Set.of(30L), Set.of(),
                1L, Instant.parse("2026-05-01T00:00:00Z"), Instant.parse("2026-05-02T00:00:00Z"));
    }

    private byte[] questionWorkbook() throws Exception {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("题库导入");
            var header = sheet.createRow(0);
            var headers = List.of("题型", "题干", "选项A", "选项B", "选项C", "选项D", "正确答案", "分值", "分类", "解析");
            for (int index = 0; index < headers.size(); index++) header.createCell(index).setCellValue(headers.get(index));
            var row = sheet.createRow(1);
            var values = List.of("单选", "灭火器使用前首先做什么？", "拔掉保险销", "乘坐电梯", "", "", "A", "100", "消防常识", "先拔销");
            for (int index = 0; index < values.size(); index++) row.createCell(index).setCellValue(values.get(index));
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
