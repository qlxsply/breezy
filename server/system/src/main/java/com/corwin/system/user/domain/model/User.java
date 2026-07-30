package com.corwin.system.user.domain.model;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * Represents a system user entity mapped to the {@code sys_user} table.
 * Encapsulates user identity, authentication, status, and lifecycle fields.
 *
 * @author Corwin 2026/1/22
 */
@Getter
@Entity
@Table(name = "sys_user", indexes = {@Index(name = "idx_sys_user_username", columnList = "username", unique = true)})
public class User {

    /** Primary key, auto-incremented. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** User type distinguishing SYSTEM, ADMIN, and regular USER accounts. */
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 16)
    private UserType userType;

    /** Unique login username. */
    @Column(name = "username", nullable = false, length = 32)
    private String username;

    /** Display nickname shown in the UI. */
    @Column(name = "nickname", nullable = false, length = 64)
    private String nickname;

    /** Hashed password value. */
    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;

    /** Algorithm used for password hashing (e.g. bcrypt). */
    @Column(name = "password_algo", nullable = false, length = 128)
    private String passwordAlgo;

    /** Current account status (ENABLED or DISABLED). */
    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false, length = 16)
    private UserStatus userStatus;

    /** Soft-delete flag; true means the user is logically deleted. */
    @Column(name = "deleted_flag", nullable = false)
    private boolean deletedFlag;

    /** Timestamp of the last password change; null forces a password change on next login. */
    @Column(name = "last_password_changed_at")
    private Instant lastPasswordChangedAt;

    /** Timestamp when the user record was created. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Identifier of the user who created this record. */
    @Column(name = "created_by", length = 64)
    private String createdBy;

    /** Timestamp when the user record was last updated. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Identifier of the user who last updated this record. */
    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    protected User() {
    }

    public User(UserType userType, String username, String nickname, String passwordHash, String passwordAlgo,
            UserStatus status, boolean deletedFlag, Instant lastPasswordChangedAt, String operator) {
        this.userType = userType;
        this.username = username;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.passwordAlgo = passwordAlgo;
        this.userStatus = status;
        this.deletedFlag = deletedFlag;
        this.lastPasswordChangedAt = lastPasswordChangedAt;
        this.createdAt = HighDate.mockInstant();
        this.updatedAt = this.createdAt;
        this.createdBy = operator;
        this.updatedBy = operator;
    }

    /**
     * Updates the user's nickname and records the operator.
     *
     * @param nickname the new nickname
     * @param operator the operator identifier
     */
    public void updateNickname(String nickname, String operator) {
        this.nickname = nickname;
        touch(operator);
    }

    /**
     * Updates the user's status and records the operator.
     *
     * @param status   the new status
     * @param operator the operator identifier
     */
    public void updateStatus(UserStatus status, String operator) {
        this.userStatus = status;
        touch(operator);
    }

    /**
     * Updates the password hash and algorithm, and marks the password change timestamp.
     *
     * @param passwordHash the new password hash
     * @param passwordAlgo the algorithm used for hashing
     * @param operator     the operator identifier
     */
    public void updatePassword(String passwordHash, String passwordAlgo, String operator) {
        this.passwordHash = passwordHash;
        this.passwordAlgo = passwordAlgo;
        this.lastPasswordChangedAt = HighDate.mockInstant();
        touch(operator);
    }

    /**
     * Resets the password and clears the last password changed timestamp,
     * forcing the user to change password on next login.
     *
     * @param passwordHash the new password hash
     * @param passwordAlgo the algorithm used for hashing
     * @param operator     the operator identifier
     */
    public void resetPassword(String passwordHash, String passwordAlgo, String operator) {
        this.passwordHash = passwordHash;
        this.passwordAlgo = passwordAlgo;
        this.lastPasswordChangedAt = null;
        touch(operator);
    }

    /**
     * Marks the user as deleted (soft delete).
     *
     * @param operator the operator identifier
     */
    public void delete(String operator) {
        this.deletedFlag = true;
        touch(operator);
    }

    /**
     * Returns the user's login account name (username).
     *
     * @return the username
     */
    public String getUserAccount() {
        return username;
    }

    /**
     * Returns whether the user must change their password (last change timestamp is null).
     *
     * @return true if password change is required
     */
    public boolean isMustChangePassword() {
        return lastPasswordChangedAt == null;
    }

    /**
     * Sets whether the user must change their password on next login.
     *
     * @param mustChangePassword true to require a password change, false to clear the requirement
     * @param operator           the operator identifier
     */
    public void markMustChangePassword(boolean mustChangePassword, String operator) {
        this.lastPasswordChangedAt = mustChangePassword ? null : HighDate.mockInstant();
        touch(operator);
    }

    private void touch(String operator) {
        this.updatedAt = HighDate.mockInstant();
        this.updatedBy = operator;
    }
}
