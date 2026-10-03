package com.uptiq;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.uptiq.redis.RedisMessageFields.*;

@Service
public class WebsiteCheckService {

    private final HttpClient httpClient;
    private final WorkerProperties properties;

    public WebsiteCheckService(WorkerProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getHttpConnectTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Map<String, String> check(String messageId, String websiteId, String url) {
        long start = System.nanoTime();
        Instant checkedAt = Instant.now();
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("Only HTTP and HTTPS URLs are supported");
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofMillis(properties.getHttpRequestTimeoutMs()))
                    .header("User-Agent", "Uptiq-Monitor/1.0")
                    .GET()
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            long responseTimeMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
            int statusCode = response.statusCode();
            boolean isUp = statusCode >= 200 && statusCode < 400;

            Map<String, String> result = new HashMap<>();

            result.put(SOURCE_MESSAGE_ID, messageId);
            result.put(WEBSITE_ID, websiteId);
            result.put(URL, url);
            result.put(STATUS, isUp ? "UP" : "DOWN");
            result.put(STATUS_CODE, String.valueOf(statusCode));
            result.put(RESPONSE_TIME_MS, String.valueOf(responseTimeMs));
            result.put(CHECKED_AT, checkedAt.toString());
            result.put(REGION_ID, properties.getRegionId());
            result.put(WORKER_ID, properties.getWorkerId());

            if (!isUp) {
                result.put(ERROR_MESSAGE, "HTTP status " + statusCode);
            }
            return result;

        } catch (Exception e) {
            long responseTimeMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

            Map<String, String> result = new HashMap<>();

            result.put(SOURCE_MESSAGE_ID, messageId);
            result.put(WEBSITE_ID, websiteId);
            result.put(URL, url);
            result.put(STATUS, "DOWN");
            result.put(STATUS_CODE, "0");
            result.put(RESPONSE_TIME_MS, String.valueOf(responseTimeMs));
            result.put(CHECKED_AT, checkedAt.toString());
            result.put(REGION_ID, properties.getRegionId());
            result.put(WORKER_ID, properties.getWorkerId());

            result.put(ERROR_MESSAGE, getErrorMessage(e));
            return result;
        }
    }

    private String getErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}