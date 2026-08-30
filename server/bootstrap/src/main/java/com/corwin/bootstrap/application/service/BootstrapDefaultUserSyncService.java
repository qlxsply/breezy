package com.corwin.bootstrap.application.service;

import com.corwin.bootstrap.application.BootstrapTaskKey;
import com.corwin.bootstrap.application.BootstrapTaskReport;
import com.corwin.system.user.domain.model.DefaultUser;
import com.corwin.system.user.domain.model.UserStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

/**
 * @author Corwin 2026/5/6
 */
@Service
@RequiredArgsConstructor
public class BootstrapDefaultUserSyncService {

  private static final String DEFAULT_ADMIN_PASSWORD = "123456";
  private static final String PASSWORD_ALGO = "bcrypt";
  private static final String OPERATOR = DefaultUser.SYSTEM.account();

  private final DataSource dataSource;
  private final BootstrapSqlTemplateService sqlTemplateService;

  public BootstrapTaskReport run(boolean dryRun) {
    long startedAt = System.currentTimeMillis();
    List<UserSeed> seeds = buildSeeds();
    UserSyncSummary summary =
        dryRun
            ? BootstrapJdbcTransactionSupport.execute(
                dataSource, connection -> inspect(connection, seeds))
            : BootstrapJdbcTransactionSupport.execute(
                dataSource, connection -> synchronize(connection, seeds));

    String message =
        "total="
            + seeds.size()
            + "; created="
            + summary.created()
            + "; skipped="
            + summary.skipped()
            + "; adminPassword="
            + DEFAULT_ADMIN_PASSWORD;
    return new BootstrapTaskReport(
        BootstrapTaskKey.DEFAULT_USER_SYNC,
        dryRun,
        true,
        System.currentTimeMillis() - startedAt,
        message);
  }

  private UserSyncSummary inspect(Connection connection, List<UserSeed> seeds) throws SQLException {
    Set<Long> existingIds = loadExistingIds(connection);
    int created = 0;
    int skipped = 0;
    for (UserSeed seed : seeds) {
      if (existingIds.contains(seed.id())) {
        skipped++;
        continue;
      }
      created++;
    }
    return new UserSyncSummary(created, skipped);
  }

  private UserSyncSummary synchronize(Connection connection, List<UserSeed> seeds)
      throws SQLException {
    Set<Long> existingIds = loadExistingIds(connection);
    int created = 0;
    int skipped = 0;
    for (UserSeed seed : seeds) {
      if (existingIds.contains(seed.id())) {
        skipped++;
        continue;
      }
      insertUser(connection, seed);
      created++;
    }
    resetIdentity(connection);
    return new UserSyncSummary(created, skipped);
  }

  private Set<Long> loadExistingIds(Connection connection) throws SQLException {
    LinkedHashSet<Long> result = new LinkedHashSet<>();
    String sql = sqlTemplateService.load("user_select_existing.sql");
    try (PreparedStatement ps = connection.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        result.add(rs.getLong("id"));
      }
    }
    return result;
  }

  private void insertUser(Connection connection, UserSeed seed) throws SQLException {
    String sql = sqlTemplateService.load("user_insert.sql");
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      bindUser(ps, seed);
      ps.executeUpdate();
    }
  }

  private void resetIdentity(Connection connection) throws SQLException {
    String sql = sqlTemplateService.load("user_reset_identity.sql");
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.executeUpdate();
    }
  }

  private void bindUser(PreparedStatement ps, UserSeed seed) throws SQLException {
    ps.setLong(1, seed.id());
    ps.setString(2, seed.userType());
    ps.setString(3, seed.username());
    ps.setString(4, seed.nickname());
    ps.setString(5, seed.passwordHash());
    ps.setString(6, seed.passwordAlgo());
    ps.setString(7, seed.userStatus());
    ps.setBoolean(8, seed.deletedFlag());
    ps.setString(9, seed.operator());
    ps.setString(10, seed.operator());
  }

  private List<UserSeed> buildSeeds() {
    List<UserSeed> result = new ArrayList<>();
    for (DefaultUser defaultUser : DefaultUser.values()) {
      String rawPassword =
          defaultUser == DefaultUser.ADMIN ? DEFAULT_ADMIN_PASSWORD : randomSystemPassword();
      result.add(
          new UserSeed(
              defaultUser.id(),
              defaultUser.account(),
              defaultUser.account(),
              defaultUser.userType().name(),
              BCrypt.hashpw(rawPassword, BCrypt.gensalt()),
              PASSWORD_ALGO,
              UserStatus.ENABLED.name(),
              false,
              OPERATOR));
    }
    return List.copyOf(result);
  }

  private String randomSystemPassword() {
    return "sys-" + UUID.randomUUID().toString().replace("-", "") + "-Aa1!";
  }

  private record UserSeed(
      Long id,
      String username,
      String nickname,
      String userType,
      String passwordHash,
      String passwordAlgo,
      String userStatus,
      boolean deletedFlag,
      String operator) {}

  private record UserSyncSummary(int created, int skipped) {}
}
