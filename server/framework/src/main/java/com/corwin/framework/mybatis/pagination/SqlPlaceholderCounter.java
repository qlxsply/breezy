package com.corwin.framework.mybatis.pagination;

/**
 * SQL JDBC 占位符计数器。
 *
 * @author Corwin 2026/7/28
 */
public final class SqlPlaceholderCounter {

    private SqlPlaceholderCounter() {
    }

    public static int count(String sql) {
        if (sql == null || sql.isEmpty()) {
            return 0;
        }

        State state = State.NORMAL;
        int count = 0;

        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

            switch (state) {
                case NORMAL -> {
                    if (current == '\'') {
                        state = State.SINGLE_QUOTE;
                    } else if (current == '"') {
                        state = State.DOUBLE_QUOTE;
                    } else if (current == '`') {
                        state = State.BACKTICK;
                    } else if (current == '-' && next == '-') {
                        state = State.LINE_COMMENT;
                        i++;
                    } else if (current == '#') {
                        state = State.LINE_COMMENT;
                    } else if (current == '/' && next == '*') {
                        state = State.BLOCK_COMMENT;
                        i++;
                    } else if (current == '?') {
                        count++;
                    }
                }

                case SINGLE_QUOTE -> {
                    if (current == '\\') {
                        i++;
                    } else if (current == '\'') {
                        if (next == '\'') {
                            i++;
                        } else {
                            state = State.NORMAL;
                        }
                    }
                }

                case DOUBLE_QUOTE -> {
                    if (current == '\\') {
                        i++;
                    } else if (current == '"') {
                        if (next == '"') {
                            i++;
                        } else {
                            state = State.NORMAL;
                        }
                    }
                }

                case BACKTICK -> {
                    if (current == '`') {
                        if (next == '`') {
                            i++;
                        } else {
                            state = State.NORMAL;
                        }
                    }
                }

                case LINE_COMMENT -> {
                    if (current == '\n' || current == '\r') {
                        state = State.NORMAL;
                    }
                }

                case BLOCK_COMMENT -> {
                    if (current == '*' && next == '/') {
                        state = State.NORMAL;
                        i++;
                    }
                }
            }
        }

        return count;
    }

    private enum State {
        NORMAL,
        SINGLE_QUOTE,
        DOUBLE_QUOTE,
        BACKTICK,
        LINE_COMMENT,
        BLOCK_COMMENT
    }
}
