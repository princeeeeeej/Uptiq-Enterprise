package com.uptiq.redis;

public final class RedisMessageFields {

    private RedisMessageFields() {
    }

    // Monitoring fields
    public static final String WEBSITE_ID = "websiteId";
    public static final String URL = "url";
    public static final String REGION_ID = "regionId";
    public static final String STATUS = "status";
    public static final String STATUS_CODE = "statusCode";
    public static final String RESPONSE_TIME_MS = "responseTimeMs";
    public static final String CHECKED_AT = "checkedAt";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String WORKER_ID = "workerId";
    public static final String SOURCE_MESSAGE_ID = "sourceMessageId";

    // Alert fields
    public static final String WEBSITE_NAME = "websiteName";
    public static final String WEBSITE_URL = "websiteUrl";
    public static final String EVENT_TYPE = "eventType";
    public static final String REASON = "reason";
    public static final String DAYS_REMAINING = "daysRemaining";

    public static final String RETRY_COUNT = "retryCount";
    public static final String ORIGINAL_STREAM = "originalStream";
    public static final String ORIGINAL_MESSAGE_ID = "originalMessageId";
    public static final String LAST_ERROR = "lastError";
}