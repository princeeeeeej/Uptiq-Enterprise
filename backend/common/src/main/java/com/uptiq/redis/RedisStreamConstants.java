package com.uptiq.redis;

public final class RedisStreamConstants {

    private RedisStreamConstants() {
    }

    public static final String CHECK_STREAM = "uptiq:checks";

    public static final String DB_WRITE_STREAM = "uptiq:db-write";

    public static final String ALERT_STREAM = "uptiq:alerts";

    public static final String FAILED_STREAM = "uptiq:failed";
}