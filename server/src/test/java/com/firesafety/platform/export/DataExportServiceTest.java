package com.firesafety.platform.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.organization.Enterprise;
import com.firesafety.platform.organization.EnterpriseRepository;
import com.firesafety.platform.organization.UserAccount;
import com.firesafety.platform.organization.UserAccountRepository;
import com.firesafety.platform.repair.CreateRepairCommand;
import com.firesafety.platform.repair.RepairTicket;
import com.firesafety.platform.repair.RepairTicketRepository;
import com.firesafety.platform.repair.RepairUrgency;
import com.firesafety.platform.training.TrainingAnswerDetail;
import com.firesafety.platform.training.TrainingAnswerDetailRepository;
import com.firesafety.platform.training.TrainingRecord;
import com.firesafety.platform.training.TrainingRecordRepository;
import com.firesafety.platform.training.TrainingTask;
import com.firesafety.platform.training.TrainingTaskRepository;
import com.firesafety.platform.training.TrainingTaskStatus;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataExportServiceTest {
    @Mock private RepairTicketRepository repairs;
    @Mock private TrainingRecordRepository records;
    @Mock private TrainingAnswerDetailRepository answerDetails;
    @Mock private TrainingTaskRepository tasks;
    @Mock private EnterpriseRepository enterprises;
    @Mock private UserAccountRepository users;
    private final SessionPrincipal operator =
            new SessionPrincipal(1L, UserRole.PLATFORM_OPERATOR, null, "平台运营");

    @Test
    void exportsRepairWorkbookAndNeutralizesFormulaLikeText() throws Exception {
        var enterprise = enterprise();
        var reporter = user(enterprise.id());
        var ticket = RepairTicket.create(enterprise.id(), reporter.id(), new CreateRepairCommand(
                RepairUrgency.HIGH, "消防设施", "一号楼", "=HYPERLINK(\"https://bad\")",
                "张三", "13800000000"));
        ticket.assignId(9L);
        when(repairs.findAll()).thenReturn(List.of(ticket));
        when(enterprises.findAll()).thenReturn(List.of(enterprise));
        when(users.findAll()).thenReturn(List.of(reporter));

        var bytes = service().exportRepairs(operator);

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("报修数据");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getRow(1).getCell(6).getStringCellValue()).startsWith("'=");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("示例总部");
        }
    }

    @Test
    void exportsTrainingRecordsAndAnswerDetailsInOneWorkbook() throws Exception {
        var now = Instant.parse("2026-06-30T02:00:00Z");
        var enterprise = enterprise();
        var user = user(enterprise.id());
        var task = TrainingTask.restore(4L, "消防基础培训", null, now.minusSeconds(60), now.plusSeconds(3600),
                60, 2, TrainingTaskStatus.PUBLISHED, Set.of(1L), Set.of(enterprise.id()), Set.of(),
                operator.userId(), now.minusSeconds(120), now.minusSeconds(30));
        var record = TrainingRecord.restore(5L, task.id(), user.id(), enterprise.id(), 80, true, 1, now);
        var detail = new TrainingAnswerDetail(6L, record.id(), 1L, Set.of("A"), true, 10);
        when(records.findAll()).thenReturn(List.of(record));
        when(answerDetails.findByRecordId(record.id())).thenReturn(List.of(detail));
        when(tasks.findAll()).thenReturn(List.of(task));
        when(enterprises.findAll()).thenReturn(List.of(enterprise));
        when(users.findAll()).thenReturn(List.of(user));

        var bytes = service().exportTrainingRecords(operator);

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheet("培训记录").getRow(1).getCell(1).getStringCellValue())
                    .isEqualTo("消防基础培训");
            assertThat(workbook.getSheet("培训记录").getRow(1).getCell(7).getStringCellValue())
                    .isEqualTo("2026-06-30 10:00:00");
            assertThat(workbook.getSheet("答题明细").getRow(1).getCell(4).getStringCellValue())
                    .isEqualTo("A");
        }
    }

    @Test
    void rejectsEnterpriseUserExport() {
        var employee = new SessionPrincipal(11L, UserRole.EMPLOYEE, 20L, "员工");

        assertThatThrownBy(() -> service().exportRepairs(employee))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code()).isEqualTo("FORBIDDEN"));
    }

    private DataExportService service() {
        return new DataExportService(repairs, records, answerDetails, tasks, enterprises, users);
    }

    private Enterprise enterprise() {
        var value = Enterprise.headquarters("示例总部", "联系人", "13800000000");
        value.assignId(20L);
        return value;
    }

    private UserAccount user(Long enterpriseId) {
        var value = UserAccount.employee(enterpriseId, "张三", "13800000000", UserRole.EMPLOYEE);
        value.assignId(11L);
        return value;
    }
}
