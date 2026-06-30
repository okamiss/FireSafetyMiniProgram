package com.firesafety.platform.repair;

import com.firesafety.platform.auth.SessionPrincipal;
import com.firesafety.platform.common.BusinessException;
import com.firesafety.platform.file.FileStorage;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Transactional
public class RepairAttachmentService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final int MAX_ATTACHMENTS = 6;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final RepairService repairs;
    private final RepairAttachmentRepository attachments;
    private final FileStorage storage;

    public RepairAttachmentService(
            RepairService repairs, RepairAttachmentRepository attachments, FileStorage storage) {
        this.repairs = repairs;
        this.attachments = attachments;
        this.storage = storage;
    }

    public RepairAttachment upload(SessionPrincipal principal, Long repairId, MultipartFile file) {
        var ticket = repairs.detail(principal, repairId);
        if (!ticket.reporterUserId().equals(principal.userId())) {
            throw new BusinessException("FORBIDDEN", "只有报修人可以上传现场照片", HttpStatus.FORBIDDEN);
        }
        if (ticket.status() != RepairStatus.PENDING_ACCEPTANCE) {
            throw new BusinessException("REPAIR_NOT_EDITABLE", "工单受理后不能继续上传照片");
        }
        if (attachments.findByRepairId(repairId).size() >= MAX_ATTACHMENTS) {
            throw new BusinessException("ATTACHMENT_LIMIT", "每个报修工单最多上传 6 张照片");
        }
        var contentType = file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException("INVALID_FILE_TYPE", "仅支持 JPEG、PNG 或 WebP 图片");
        }
        if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("INVALID_FILE_SIZE", "图片不能为空且不能超过 10MB");
        }
        try {
            var bytes = file.getBytes();
            if (!matchesSignature(contentType, bytes)) {
                throw new BusinessException("INVALID_FILE_CONTENT", "图片内容与文件类型不匹配");
            }
            var stored = storage.store(bytes, file.getOriginalFilename(), contentType);
            return attachments.save(new RepairAttachment(
                    null, ticket.enterpriseId(), repairId, principal.userId(), stored.storageKey(),
                    stored.originalName(), stored.contentType(), stored.size(), Instant.now()));
        } catch (IOException exception) {
            throw new BusinessException("FILE_UPLOAD_FAILED", "图片读取失败");
        }
    }

    @Transactional(readOnly = true)
    public List<RepairAttachment> list(SessionPrincipal principal, Long repairId) {
        repairs.detail(principal, repairId);
        return attachments.findByRepairId(repairId);
    }

    @Transactional(readOnly = true)
    public RepairAttachmentFile download(SessionPrincipal principal, Long attachmentId) {
        var metadata = attachments.findById(attachmentId)
                .orElseThrow(() -> new BusinessException(
                        "ATTACHMENT_NOT_FOUND", "报修照片不存在", HttpStatus.NOT_FOUND));
        repairs.detail(principal, metadata.repairId());
        return new RepairAttachmentFile(metadata, storage.load(metadata.storageKey()));
    }

    private boolean matchesSignature(String contentType, byte[] bytes) {
        return switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
            case "image/png" -> bytes.length >= 4
                    && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47;
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }
}
