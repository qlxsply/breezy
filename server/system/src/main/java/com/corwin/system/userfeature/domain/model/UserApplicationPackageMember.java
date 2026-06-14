package com.corwin.system.userfeature.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 用户应用包成员。
 *
 * <p>表示用户与用户应用包之间的成员关系。</p>
 *
 * @author Corwin
 */
@Getter
@Entity
@Table(name = "sys_user_application_package_member", indexes = {
        @Index(name = "idx_sys_user_application_package_member_unique", columnList = "package_id,user_id",
                unique = true),
        @Index(name = "idx_sys_user_application_package_member_package_id", columnList = "package_id"),
        @Index(name = "idx_sys_user_application_package_member_user_id", columnList = "user_id")})
public class UserApplicationPackageMember {

    /**
     * 主键 ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户应用包 ID。
     *
     * <p>逻辑引用 {@link UserApplicationPackage#getId()}，数据库层面不创建外键。</p>
     */
    @Column(name = "package_id", nullable = false)
    private Long packageId;

    /**
     * 用户 ID。
     *
     * <p>逻辑引用用户表主键，数据库层面不创建外键。</p>
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 创建人用户 ID。
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 创建时间。
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * JPA 构造方法。
     */
    protected UserApplicationPackageMember() {
    }

    /**
     * 创建用户应用包成员关系。
     *
     * @param packageId 用户应用包 ID
     * @param userId    用户 ID
     * @param operator  操作人用户 ID
     */
    public UserApplicationPackageMember(Long packageId, Long userId, Long operator) {
        this.packageId = packageId;
        this.userId = userId;
        this.createdBy = operator;
        this.createdAt = HighDate.mockInstant();
    }
}
