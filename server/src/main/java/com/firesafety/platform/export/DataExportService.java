package com.firesafety.platform.export;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.organization.Enterprise;
import com.firesafety.platform.organization.EnterpriseRepository;
import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import com.firesafety.platform.repair.RepairStatus;
import com.firesafety.platform.repair.RepairTicketRepository;
import com.firesafety.platform.repair.RepairUrgency;
import com.firesafety.platform.training.TrainingAnswerDetailRepository;
import com.firesafety.platform.training.TrainingRecordRepository;
import com.firesafety.platform.training.TrainingTask;
import com.firesafety.platform.training.TrainingTaskRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class DataExportService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(BUSINESS_ZONE);
    private static final String[] REPAIR_HEADERS = {
        "工单编号", "企业", "报修人", "紧急程度", "故障类型", "位置", "问题描述", "联系人",
        "联系电话", "状态", "处理人", "处理结果", "提交时间", "完成时间", "关闭时间"
    };
    private static final String[] TRAINING_HEADERS = {
        "记录编号", "培训任务", "企业", "用户", "成绩", "是否通过", "作答次数", "提交时间"
    };
    private static final String[] ANSWER_HEADERS = {
        "记录编号", "培训任务", "用户", "题目编号", "用户答案", "是否正确", "得分"
    };

    private final RepairTicketRepository repairs;
    private final TrainingRecordRepository records;
    private final TrainingAnswerDetailRepository answerDetails;
    private final TrainingTaskRepository tasks;
    private final EnterpriseRepository enterprises;
    private final UserAccountRepository users;

    public DataExportService(
            RepairTicketRepository repairs,
            TrainingRecordRepository records,
            TrainingAnswerDetailRepository answerDetails,
            TrainingTaskRepository tasks,
            EnterpriseRepository enterprises,
            UserAccountRepository users) {
        this.repairs = repairs;
        this.records = records;
        this.answerDetails = answerDetails;
        this.tasks = tasks;
        this.enterprises = enterprises;
        this.users = users;
    }

    public byte[] exportRepairs(SessionPrincipal principal) {
        requirePlatformRole(principal);
        var enterpriseNames = enterprises.findAll().stream()
                .collect(Collectors.toMap(Enterprise::id, Enterprise::name));
        var userNames = users.findAll().stream()
                .collect(Collectors.toMap(UserAccount::id, UserAccount::displayName));
        return createWorkbook(workbook -> {
            var sheet = workbook.createSheet("报修数据");
            writeHeader(workbook, sheet, REPAIR_HEADERS);
            int rowIndex = 1;
            for (var ticket : repairs.findAll()) {
                var row = sheet.createRow(rowIndex++);
                number(row, 0, ticket.id());
                text(row, 1, enterpriseNames.getOrDefault(ticket.enterpriseId(), "企业#" + ticket.enterpriseId()));
                text(row, 2, userNames.getOrDefault(ticket.reporterUserId(), "用户#" + ticket.reporterUserId()));
                text(row, 3, urgencyLabel(ticket.urgency()));
                text(row, 4, ticket.faultType());
                text(row, 5, ticket.location());
                text(row, 6, ticket.description());
                text(row, 7, ticket.contactName());
                text(row, 8, ticket.contactPhone());
                text(row, 9, statusLabel(ticket.status()));
                text(row, 10, ticket.handlerUserId() == null ? null
                        : userNames.getOrDefault(ticket.handlerUserId(), "用户#" + ticket.handlerUserId()));
                text(row, 11, ticket.result());
                text(row, 12, format(ticket.createdAt()));
                text(row, 13, format(ticket.completedAt()));
                text(row, 14, format(ticket.closedAt()));
            }
            finish(sheet, REPAIR_HEADERS.length);
        });
    }

    public byte[] exportTrainingRecords(SessionPrincipal principal) {
        requirePlatformRole(principal);
        Map<Long, String> enterpriseNames = enterprises.findAll().stream()
                .collect(Collectors.toMap(Enterprise::id, Enterprise::name));
        Map<Long, String> userNames = users.findAll().stream()
                .collect(Collectors.toMap(UserAccount::id, UserAccount::displayName));
        Map<Long, TrainingTask> taskValues = tasks.findAll().stream()
                .collect(Collectors.toMap(TrainingTask::id, Function.identity()));
        var recordValues = records.findAll();
        return createWorkbook(workbook -> {
            var recordSheet = workbook.createSheet("培训记录");
            writeHeader(workbook, recordSheet, TRAINING_HEADERS);
            int rowIndex = 1;
            for (var record : recordValues) {
                var row = recordSheet.createRow(rowIndex++);
                var task = taskValues.get(record.taskId());
                number(row, 0, record.id());
                text(row, 1, task == null ? "任务#" + record.taskId() : task.title());
                text(row, 2, enterpriseNames.getOrDefault(
                        record.enterpriseId(), "企业#" + record.enterpriseId()));
                text(row, 3, userNames.getOrDefault(record.userId(), "用户#" + record.userId()));
                number(row, 4, record.score());
                text(row, 5, record.passed() ? "是" : "否");
                number(row, 6, record.attemptNo());
                text(row, 7, format(record.submittedAt()));
            }
            finish(recordSheet, TRAINING_HEADERS.length);

            var detailSheet = workbook.createSheet("答题明细");
            writeHeader(workbook, detailSheet, ANSWER_HEADERS);
            int detailRowIndex = 1;
            for (var record : recordValues) {
                var task = taskValues.get(record.taskId());
                for (var detail : answerDetails.findByRecordId(record.id())) {
                    var row = detailSheet.createRow(detailRowIndex++);
                    number(row, 0, record.id());
                    text(row, 1, task == null ? "任务#" + record.taskId() : task.title());
                    text(row, 2, userNames.getOrDefault(record.userId(), "用户#" + record.userId()));
                    number(row, 3, detail.questionId());
                    text(row, 4, detail.userAnswers().stream().sorted().collect(Collectors.joining(",")));
                    text(row, 5, detail.correct() ? "是" : "否");
                    number(row, 6, detail.awardedScore());
                }
            }
            finish(detailSheet, ANSWER_HEADERS.length);
        });
    }

    private byte[] createWorkbook(Consumer<XSSFWorkbook> writer) {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            writer.accept(workbook);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException("EXPORT_FAILED", "Excel 导出生成失败");
        }
    }

    private void writeHeader(Workbook workbook, Sheet sheet, String[] headers) {
        var font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        var style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        var row = sheet.createRow(0);
        for (int index = 0; index < headers.length; index++) {
            var cell = row.createCell(index);
            cell.setCellValue(headers[index]);
            cell.setCellStyle(style);
        }
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, headers.length - 1));
    }

    private void finish(Sheet sheet, int columns) {
        for (int index = 0; index < columns; index++) {
            sheet.autoSizeColumn(index);
            sheet.setColumnWidth(index, Math.min(sheet.getColumnWidth(index) + 800, 16_000));
        }
    }

    private void text(org.apache.poi.ss.usermodel.Row row, int column, String value) {
        row.createCell(column).setCellValue(safeText(value));
    }

    private void number(org.apache.poi.ss.usermodel.Row row, int column, Number value) {
        if (value == null) text(row, column, null);
        else row.createCell(column).setCellValue(value.doubleValue());
    }

    private String safeText(String value) {
        if (value == null) return "";
        var leadingTrimmed = value.stripLeading();
        if (!leadingTrimmed.isEmpty() && "=+-@".indexOf(leadingTrimmed.charAt(0)) >= 0) return "'" + value;
        return value;
    }

    private String format(Instant value) { return value == null ? "" : DATE_TIME.format(value); }

    private String urgencyLabel(RepairUrgency value) {
        return value == RepairUrgency.HIGH ? "紧急" : "普通";
    }

    private String statusLabel(RepairStatus value) {
        return switch (value) {
            case PENDING_ACCEPTANCE -> "待受理";
            case PROCESSING -> "处理中";
            case COMPLETED -> "已完成";
            case CLOSED -> "已关闭";
        };
    }

    private void requirePlatformRole(SessionPrincipal principal) {
        if (principal == null || (principal.role() != UserRole.SUPER_ADMIN
                && principal.role() != UserRole.PLATFORM_OPERATOR)) {
            throw new BusinessException("FORBIDDEN", "没有数据导出权限", HttpStatus.FORBIDDEN);
        }
    }
}
