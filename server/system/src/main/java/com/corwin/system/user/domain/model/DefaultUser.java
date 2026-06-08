package com.corwin.system.user.domain.model;

import com.corwin.framework.constant.UserType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/1/30
 */
public enum DefaultUser {
    SYSTEM(1L, "system", UserType.SYSTEM),
    SCHEDULER(2L, "scheduler", UserType.SYSTEM),
    EVENT(3L, "event", UserType.SYSTEM),
    SERVICE(4L, "service", UserType.SYSTEM),
    MOCK(5L, "mock", UserType.SYSTEM),
    ADMIN(1000L, "admin", UserType.INTERNAL),
    ;

    private final long id;
    private final String username;
    private final UserType userType;

    DefaultUser(long id, String username, UserType userType) {
        this.id = id;
        this.username = username;
        this.userType = userType;
    }

    public long id() {
        return id;
    }

    public String account() {
        return username;
    }

    public UserType userType() {
        return userType;
    }

    public static List<DefaultUser> systemUsers() {
        return Cache.SYSTEM_USERS_LIST;
    }

    public static Optional<DefaultUser> byId(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(Cache.BY_ID.get(userId));
    }

    public static boolean isSystemUser(Long userId) {
        if (userId == null) {
            return false;
        }
        return Cache.SYSTEM_USER_IDS.contains(userId);
    }

    public static boolean isAdmin(Long userId) {
        return userId != null && userId == ADMIN.id;
    }

    public static boolean isReserved(Long userId) {
        if (userId == null) {
            return false;
        }
        return Cache.RESERVED_IDS.contains(userId);
    }

    private static final class Cache {
        private static final EnumSet<DefaultUser> SYSTEM_USERS_SET = Arrays.stream(DefaultUser.values())
                .filter(u -> u.userType == UserType.SYSTEM)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DefaultUser.class)));

        private static final List<DefaultUser> SYSTEM_USERS_LIST = List.copyOf(SYSTEM_USERS_SET);

        private static final Map<Long, DefaultUser> BY_ID = Arrays.stream(DefaultUser.values())
                .collect(Collectors.toUnmodifiableMap(DefaultUser::id, Function.identity()));

        private static final Set<Long> RESERVED_IDS = BY_ID.keySet();

        private static final Set<Long> SYSTEM_USER_IDS = SYSTEM_USERS_SET.stream().map(DefaultUser::id)
                .collect(Collectors.toUnmodifiableSet());
    }

}
