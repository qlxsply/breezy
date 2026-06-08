package com.corwin.jsonfmt.application.service;

import com.corwin.config.BusinessConfigKeys;
import com.corwin.framework.config.ConfigRegistry;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.json.Json;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.util.StrUtil;
import com.corwin.jsonfmt.application.command.JsonFmtRecordBatchDeleteCommand;
import com.corwin.jsonfmt.application.command.JsonFmtRecordRenameCommand;
import com.corwin.jsonfmt.application.command.JsonFmtRecordReorderCommand;
import com.corwin.jsonfmt.application.command.JsonFmtRecordSaveCommand;
import com.corwin.jsonfmt.application.view.JsonFmtRecordDetailView;
import com.corwin.jsonfmt.application.view.JsonFmtRecordListItemView;
import com.corwin.jsonfmt.domain.error.JsonFmtError;
import com.corwin.jsonfmt.domain.model.JsonFmtRecord;
import com.corwin.jsonfmt.domain.repo.JsonFmtRecordRepository;
import com.corwin.system.file.application.port.FileCommandPort;
import com.corwin.system.file.published.FilePurpose;
import com.corwin.system.file.published.InternalFileType;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author Corwin 2026/3/2
 */
@Service
@RequiredArgsConstructor
public class JsonFmtRecordAppService {

    private static final int NAME_MAX_LENGTH = 128;
    private static final int DEFAULT_CONTENT_FILE_THRESHOLD = 61440;
    private static final DateTimeFormatter DEFAULT_NAME_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JsonFmtRecordRepository recordRepository;
    private final FileCommandPort fileService;


    public List<JsonFmtRecordListItemView> list(Long userId, String keyword) {
        BizAssert.notNull(userId, BaseError.FORBIDDEN);
        String normalizedKeyword = StrUtil.trimToNull(keyword);
        List<JsonFmtRecord> records = normalizedKeyword == null ? recordRepository.findByUserIdOrderByOrderNoAsc(
                userId) : recordRepository.findByUserIdAndNameContainingIgnoreCaseOrderByOrderNoAsc(userId,
                normalizedKeyword);
        return records.stream().map(this::toListItemView).toList();
    }

    public JsonFmtRecordDetailView detail(Long userId, String recordId) {
        BizAssert.notNull(userId, BaseError.FORBIDDEN);
        BizAssert.notBlank(recordId, BaseError.MISSING_PARAMETER);
        JsonFmtRecord record = recordRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new BizException(JsonFmtError.JSONFMT_RECORD_NOT_FOUND));
        return toDetailView(record);
    }

    @Transactional
    public JsonFmtRecordDetailView save(Long userId, JsonFmtRecordSaveCommand command) {
        BizAssert.notNull(userId, BaseError.FORBIDDEN);
        BizAssert.notNull(command, BaseError.MISSING_PARAMETER);

        String content = normalizeContent(command.content());
        validateContent(content);
        boolean storeInFile = shouldStoreInFile(content);

        String recordId = StrUtil.trimToNull(command.id());
        if (recordId == null) {
            String name = resolveNewRecordName(userId, command.name());
            if (storeInFile) {
                String newFileId = createContentFile(content);
                JsonFmtRecord record = JsonFmtRecord.createWithFile(userId, name, newFileId, 0);
                try {
                    return toDetailView(createAsTop(record));
                } catch (RuntimeException ex) {
                    cleanupCreatedFile(newFileId);
                    throw ex;
                }
            }
            JsonFmtRecord record = JsonFmtRecord.createInline(userId, name, content, 0);
            return toDetailView(createAsTop(record));
        }

        JsonFmtRecord record = recordRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new BizException(JsonFmtError.JSONFMT_RECORD_NOT_FOUND));
        String trimmedName = trimToNull(command.name());
        if (command.name() != null) {
            if (trimmedName == null) {
                BizAssert.fail(JsonFmtError.JSONFMT_RECORD_NAME_REQUIRED);
            }
            validateName(trimmedName);
            record.rename(trimmedName);
        }
        String oldFileId = record.getContentFileId();
        String newFileId = null;
        if (storeInFile) {
            newFileId = createContentFile(content);
            record.updateFileContent(newFileId);
        } else {
            record.updateInlineContent(content);
        }
        try {
            recordRepository.save(record);
        } catch (RuntimeException ex) {
            cleanupCreatedFile(newFileId);
            throw ex;
        }
        cleanupOldFile(oldFileId, record.getContentFileId());
        return toDetailView(record);
    }

    @Transactional
    public void rename(Long userId, JsonFmtRecordRenameCommand command) {
        BizAssert.notNull(userId, BaseError.FORBIDDEN);
        BizAssert.notNull(command, BaseError.MISSING_PARAMETER);
        BizAssert.notBlank(command.id(), BaseError.MISSING_PARAMETER);

        String name = trimToNull(command.name());
        if (name == null) {
            BizAssert.fail(JsonFmtError.JSONFMT_RECORD_NAME_REQUIRED);
        }
        validateName(name);

        JsonFmtRecord record = recordRepository.findByIdAndUserId(command.id(), userId)
                .orElseThrow(() -> new BizException(JsonFmtError.JSONFMT_RECORD_NOT_FOUND));
        record.rename(name);
        recordRepository.save(record);
    }

    @Transactional
    public void reorder(Long userId, JsonFmtRecordReorderCommand command) {
        BizAssert.notNull(userId, BaseError.FORBIDDEN);
        BizAssert.notNull(command, BaseError.MISSING_PARAMETER);

        List<String> orderedIds = command.orderedIds();
        BizAssert.notEmpty(orderedIds, JsonFmtError.JSONFMT_RECORD_ORDER_INVALID);

        List<String> normalizedIds = new ArrayList<>(orderedIds.size());
        LinkedHashSet<String> uniqueIds = new LinkedHashSet<>();
        for (String id : orderedIds) {
            String trimmed = trimToNull(id);
            if (trimmed == null) {
                BizAssert.fail(JsonFmtError.JSONFMT_RECORD_ORDER_INVALID);
            }
            normalizedIds.add(trimmed);
            uniqueIds.add(trimmed);
        }
        if (uniqueIds.size() != normalizedIds.size()) {
            BizAssert.fail(JsonFmtError.JSONFMT_RECORD_ORDER_INVALID);
        }

        List<JsonFmtRecord> records = recordRepository.findByUserIdAndIdIn(userId, uniqueIds);
        if (records.size() != uniqueIds.size()) {
            BizAssert.fail(JsonFmtError.JSONFMT_RECORD_ORDER_INVALID);
        }
        Map<String, JsonFmtRecord> recordMap = new LinkedHashMap<>();
        for (JsonFmtRecord record : records) {
            recordMap.put(record.getId(), record);
        }

        List<JsonFmtRecord> reordered = new ArrayList<>(normalizedIds.size());
        for (int i = 0; i < normalizedIds.size(); i++) {
            String id = normalizedIds.get(i);
            JsonFmtRecord record = recordMap.get(id);
            if (record == null) {
                BizAssert.fail(JsonFmtError.JSONFMT_RECORD_ORDER_INVALID);
            }
            record.reorderTo(i);
            reordered.add(record);
        }
        recordRepository.saveAll(reordered);
    }

    @Transactional
    public void delete(Long userId, String recordId) {
        BizAssert.notNull(userId, BaseError.FORBIDDEN);
        BizAssert.notBlank(recordId, BaseError.MISSING_PARAMETER);

        JsonFmtRecord record = recordRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new BizException(JsonFmtError.JSONFMT_RECORD_NOT_FOUND));
        deleteRecord(record);
        reorderAfterDelete(userId);
    }

    @Transactional
    public void batchDelete(Long userId, JsonFmtRecordBatchDeleteCommand command) {
        BizAssert.notNull(userId, BaseError.FORBIDDEN);
        BizAssert.notNull(command, BaseError.MISSING_PARAMETER);
        BizAssert.notEmpty(command.recordIds(), JsonFmtError.JSONFMT_RECORD_DELETE_INVALID);

        LinkedHashSet<String> idSet = new LinkedHashSet<>();
        for (String id : command.recordIds()) {
            String trimmed = trimToNull(id);
            if (trimmed == null) {
                BizAssert.fail(JsonFmtError.JSONFMT_RECORD_DELETE_INVALID);
            }
            idSet.add(trimmed);
        }
        BizAssert.notEmpty(idSet, JsonFmtError.JSONFMT_RECORD_DELETE_INVALID);

        List<JsonFmtRecord> records = recordRepository.findByUserIdAndIdIn(userId, idSet);
        if (records.size() != idSet.size()) {
            BizAssert.fail(JsonFmtError.JSONFMT_RECORD_NOT_FOUND);
        }

        for (JsonFmtRecord record : records) {
            deleteRecord(record);
        }
        reorderAfterDelete(userId);
    }

    private JsonFmtRecord createAsTop(JsonFmtRecord record) {
        List<JsonFmtRecord> existing = recordRepository.findByUserIdOrderByOrderNoAsc(record.getUserId());
        if (!existing.isEmpty()) {
            for (int i = 0; i < existing.size(); i++) {
                existing.get(i).reorderTo(i + 1);
            }
            recordRepository.saveAll(existing);
        }
        return recordRepository.save(record);
    }

    private void deleteRecord(JsonFmtRecord record) {
        String fileId = record.getContentFileId();
        recordRepository.delete(record);
        cleanupCreatedFile(fileId);
    }

    private void reorderAfterDelete(Long userId) {
        List<JsonFmtRecord> remain = recordRepository.findByUserIdOrderByOrderNoAsc(userId);
        boolean changed = false;
        for (int i = 0; i < remain.size(); i++) {
            JsonFmtRecord record = remain.get(i);
            if (!Objects.equals(record.getOrderNo(), i)) {
                record.reorderTo(i);
                changed = true;
            }
        }
        if (changed) {
            recordRepository.saveAll(remain);
        }
    }

    private String resolveNewRecordName(Long userId, String rawName) {
        String name = trimToNull(rawName);
        if (rawName != null && name == null) {
            BizAssert.fail(JsonFmtError.JSONFMT_RECORD_NAME_REQUIRED);
        }
        if (name == null) {
            name = generateDefaultName(userId);
        }
        validateName(name);
        return name;
    }

    private String generateDefaultName(Long userId) {
        LocalDate today = HighDate.mockDate();
        Instant startAt = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endAt = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        long seq = recordRepository.countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, startAt,
                endAt) + 1;
        return today.format(DEFAULT_NAME_DATE) + "_" + seq;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            BizAssert.fail(JsonFmtError.JSONFMT_RECORD_NAME_REQUIRED);
        }
        if (name.length() > NAME_MAX_LENGTH) {
            BizAssert.fail(JsonFmtError.JSONFMT_RECORD_NAME_TOO_LONG);
        }
    }

    private String normalizeContent(String content) {
        String trimmed = trimToNull(content);
        if (trimmed == null) {
            BizAssert.fail(JsonFmtError.JSONFMT_RECORD_CONTENT_REQUIRED);
        }
        return trimmed;
    }

    private void validateContent(String content) {
        if (!isValidJson(content)) {
            BizAssert.fail(JsonFmtError.JSONFMT_RECORD_CONTENT_INVALID);
        }
    }

    private boolean isValidJson(String content) {
        if (content == null) {
            return false;
        }
        if (canParseJson(content)) {
            return true;
        }
        String unescaped = tryUnescapeJson(content);
        if (unescaped == null) {
            return false;
        }
        return canParseJson(unescaped);
    }

    private boolean canParseJson(String content) {
        try {
            return Json.mapper().readTree(content) != null;
        } catch (JsonProcessingException ex) {
            return false;
        }
    }

    private String tryUnescapeJson(String content) {
        try {
            return Json.mapper().readValue("\"" + content + "\"", String.class);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private boolean shouldStoreInFile(String content) {
        long threshold = resolveContentFileThreshold();
        int length = content.getBytes(StandardCharsets.UTF_8).length;
        return length >= threshold;
    }

    private long resolveContentFileThreshold() {
        int threshold = ConfigRegistry.intV(BusinessConfigKeys.JSONFMT_CONTENT_FILE_THRESHOLD);
        return threshold > 0 ? threshold : DEFAULT_CONTENT_FILE_THRESHOLD;
    }

    private String createContentFile(String content) {
        String fileName = "jsonfmt_" + UUID.randomUUID();
        return fileService.createInternalFile(fileName, InternalFileType.JSON, content.getBytes(StandardCharsets.UTF_8),
                FilePurpose.JSONFMT);
    }

    private void cleanupCreatedFile(String fileId) {
        if (fileId == null) {
            return;
        }
        try {
            fileService.deleteFile(fileId);
        } catch (Exception ignored) {
        }
    }

    private void cleanupOldFile(String oldFileId, String currentFileId) {
        if (oldFileId == null || Objects.equals(oldFileId, currentFileId)) {
            return;
        }
        try {
            fileService.deleteFile(oldFileId);
        } catch (Exception ignored) {
        }
    }

    private String resolveContent(JsonFmtRecord record) {
        String fileId = record.getContentFileId();
        if (fileId == null || fileId.isBlank()) {
            return record.getContent();
        }
        byte[] content = fileService.readFileContent(fileId);
        return new String(content, StandardCharsets.UTF_8);
    }

    private String trimToNull(String value) {
        return StrUtil.trimToNull(value);
    }

    private JsonFmtRecordListItemView toListItemView(JsonFmtRecord record) {
        return new JsonFmtRecordListItemView(record.getId(), record.getName(), record.getOrderNo(),
                record.getUpdatedAt());
    }

    private JsonFmtRecordDetailView toDetailView(JsonFmtRecord record) {
        return new JsonFmtRecordDetailView(record.getId(), record.getName(), resolveContent(record),
                record.getOrderNo(), record.getCreatedAt(), record.getUpdatedAt());
    }
}
