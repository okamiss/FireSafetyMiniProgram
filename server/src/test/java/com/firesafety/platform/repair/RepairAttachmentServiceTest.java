package com.firesafety.platform.repair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.auth.UserRole;
import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.file.FileStorage;
import com.firesafety.platform.file.StoredFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class RepairAttachmentServiceTest {
    @Mock private RepairService repairs;
    private final MemoryFiles files = new MemoryFiles();
    private final FileStorage storage = (content, name, type) ->
            new StoredFile("repair/" + name, name, type, content.length);
    private final SessionPrincipal employee =
            new SessionPrincipal(11L, UserRole.EMPLOYEE, 20L, "张三");
    private final SessionPrincipal enterpriseAdmin =
            new SessionPrincipal(12L, UserRole.ENTERPRISE_ADMIN, 20L, "李主管");

    @Test
    void storesAuthorizedImageAndPersistsMetadata() {
        var ticket = ticket();
        when(repairs.detail(employee, ticket.id())).thenReturn(ticket);
        var service = new RepairAttachmentService(repairs, files, storage);

        var attachment = service.upload(employee, ticket.id(),
                new MockMultipartFile("file", "site.jpg", "image/jpeg",
                        new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 1}));

        assertThat(attachment).extracting(RepairAttachment::repairId, RepairAttachment::uploaderUserId,
                RepairAttachment::originalName).containsExactly(8L, 11L, "site.jpg");
    }

    @Test
    void rejectsSeventhAttachment() {
        var ticket = ticket();
        when(repairs.detail(employee, ticket.id())).thenReturn(ticket);
        for (int index = 0; index < 6; index++) files.save(new RepairAttachment(
                null, 20L, 8L, 11L, "key-" + index, "site.jpg", "image/jpeg", 3, null));
        var service = new RepairAttachmentService(repairs, files, storage);

        assertThatThrownBy(() -> service.upload(employee, ticket.id(),
                        new MockMultipartFile("file", "seventh.jpg", "image/jpeg", new byte[] {1})))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code()).isEqualTo("ATTACHMENT_LIMIT"));
    }

    @Test
    void rejectsNonImageContentType() {
        var ticket = ticket();
        when(repairs.detail(employee, ticket.id())).thenReturn(ticket);
        var service = new RepairAttachmentService(repairs, files, storage);

        assertThatThrownBy(() -> service.upload(employee, ticket.id(),
                        new MockMultipartFile("file", "payload.txt", "text/plain", new byte[] {1})))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code()).isEqualTo("INVALID_FILE_TYPE"));
    }

    @Test
    void rejectsUploadFromSomeoneOtherThanReporter() {
        var ticket = ticket();
        when(repairs.detail(enterpriseAdmin, ticket.id())).thenReturn(ticket);
        var service = new RepairAttachmentService(repairs, files, storage);

        assertThatThrownBy(() -> service.upload(enterpriseAdmin, ticket.id(), jpeg()))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code()).isEqualTo("FORBIDDEN"));
    }

    @Test
    void rejectsUploadAfterTicketIsAccepted() {
        var ticket = ticket();
        ticket.accept(99L);
        when(repairs.detail(employee, ticket.id())).thenReturn(ticket);
        var service = new RepairAttachmentService(repairs, files, storage);

        assertThatThrownBy(() -> service.upload(employee, ticket.id(), jpeg()))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).code()).isEqualTo("REPAIR_NOT_EDITABLE"));
    }

    private MockMultipartFile jpeg() {
        return new MockMultipartFile("file", "site.jpg", "image/jpeg",
                new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 1});
    }

    private RepairTicket ticket() {
        var ticket = RepairTicket.create(20L, 11L, new CreateRepairCommand(
                RepairUrgency.NORMAL, "消防设施", "一号楼", "故障", "张三", "13800000000"));
        ticket.assignId(8L);
        return ticket;
    }

    private static final class MemoryFiles implements RepairAttachmentRepository {
        private final List<RepairAttachment> values = new ArrayList<>();
        private long sequence = 1;
        @Override public RepairAttachment save(RepairAttachment value) {
            var saved = value.id() == null ? value.withId(sequence++) : value;
            values.add(saved); return saved;
        }
        @Override public List<RepairAttachment> findByRepairId(Long repairId) {
            return values.stream().filter(value -> value.repairId().equals(repairId)).toList();
        }
        @Override public Optional<RepairAttachment> findById(Long id) {
            return values.stream().filter(value -> value.id().equals(id)).findFirst();
        }
    }
}
