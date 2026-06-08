package com.corwin.jsonfmt.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Corwin 2026/3/1
 */
@Getter
@Entity
@Table(name = "jsonfmt_record", indexes = {
        @Index(name = "idx_jsonfmt_record_user_order", columnList = "user_id,order_no"),
        @Index(name = "idx_jsonfmt_record_user_updated_at", columnList = "user_id,updated_at")
})
public class JsonFmtRecord {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "content")
    private String content;

    @Column(name = "content_file_id", length = 36)
    private String contentFileId;

    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JsonFmtRecord() {
    }

    public static JsonFmtRecord createInline(Long userId, String name, String content, Integer orderNo) {
        JsonFmtRecord record = new JsonFmtRecord();
        record.id = UUID.randomUUID().toString();
        record.userId = normalizeUserId(userId);
        record.name = normalizeName(name);
        record.content = normalizeContent(content);
        record.contentFileId = null;
        record.orderNo = normalizeOrderNo(orderNo);
        record.createdAt = HighDate.mockInstant();
        record.updatedAt = record.createdAt;
        return record;
    }

    public static JsonFmtRecord createWithFile(Long userId, String name, String contentFileId, Integer orderNo) {
        JsonFmtRecord record = new JsonFmtRecord();
        record.id = UUID.randomUUID().toString();
        record.userId = normalizeUserId(userId);
        record.name = normalizeName(name);
        record.content = null;
        record.contentFileId = normalizeContentFileId(contentFileId);
        record.orderNo = normalizeOrderNo(orderNo);
        record.createdAt = HighDate.mockInstant();
        record.updatedAt = record.createdAt;
        return record;
    }

    public void rename(String newName) {
        this.name = normalizeName(newName);
        touch();
    }

    public void updateInlineContent(String newContent) {
        this.content = normalizeContent(newContent);
        this.contentFileId = null;
        touch();
    }

    public void updateFileContent(String newContentFileId) {
        this.contentFileId = normalizeContentFileId(newContentFileId);
        this.content = null;
        touch();
    }

    public void reorderTo(Integer newOrderNo) {
        this.orderNo = normalizeOrderNo(newOrderNo);
        touch();
    }

    private void touch() {
        this.updatedAt = HighDate.mockInstant();
    }

    private static Long normalizeUserId(Long userId) {
        if (userId == null || userId <= 0L) {
            throw new IllegalArgumentException("userId required");
        }
        return userId;
    }

    private static String normalizeName(String name) {
        String normalized = Objects.requireNonNull(name, "name required").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("name required");
        }
        return normalized;
    }

    private static String normalizeContent(String content) {
        String normalized = Objects.requireNonNull(content, "content required");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("content required");
        }
        return normalized;
    }

    private static String normalizeContentFileId(String contentFileId) {
        String normalized = Objects.requireNonNull(contentFileId, "contentFileId required").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("contentFileId required");
        }
        return normalized;
    }

    private static Integer normalizeOrderNo(Integer orderNo) {
        if (orderNo == null || orderNo < 0) {
            throw new IllegalArgumentException("orderNo invalid");
        }
        return orderNo;
    }
}
