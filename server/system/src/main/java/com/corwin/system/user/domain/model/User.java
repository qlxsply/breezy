package com.corwin.system.user.domain.model;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * @author Corwin 2026/1/22
 */
@Getter
@Entity
@Table(name = "sys_user", indexes = {@Index(name = "idx_sys_user_username", columnList = "username", unique = true)})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 16)
    private UserType userType;

    @Column(name = "username", nullable = false, length = 32)
    private String username;

    @Column(name = "nickname", nullable = false, length = 64)
    private String nickname;

    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;

    @Column(name = "password_algo", nullable = false, length = 128)
    private String passwordAlgo;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false, length = 16)
    private UserStatus userStatus;

    @Column(name = "deleted_flag", nullable = false)
    private boolean deletedFlag;

    @Column(name = "last_password_changed_at")
    private Instant lastPasswordChangedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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

    public void updateNickname(String nickname, String operator) {
        this.nickname = nickname;
        touch(operator);
    }

    public void updateStatus(UserStatus status, String operator) {
        this.userStatus = status;
        touch(operator);
    }

    public void updatePassword(String passwordHash, String passwordAlgo, String operator) {
        this.passwordHash = passwordHash;
        this.passwordAlgo = passwordAlgo;
        this.lastPasswordChangedAt = HighDate.mockInstant();
        touch(operator);
    }

    public void resetPassword(String passwordHash, String passwordAlgo, String operator) {
        this.passwordHash = passwordHash;
        this.passwordAlgo = passwordAlgo;
        this.lastPasswordChangedAt = null;
        touch(operator);
    }

    public void delete(String operator) {
        this.deletedFlag = true;
        touch(operator);
    }

    public String getUserAccount() {
        return username;
    }

    public boolean isMustChangePassword() {
        return lastPasswordChangedAt == null;
    }

    public void markMustChangePassword(boolean mustChangePassword, String operator) {
        this.lastPasswordChangedAt = mustChangePassword ? null : HighDate.mockInstant();
        touch(operator);
    }

    private void touch(String operator) {
        this.updatedAt = HighDate.mockInstant();
        this.updatedBy = operator;
    }
}
