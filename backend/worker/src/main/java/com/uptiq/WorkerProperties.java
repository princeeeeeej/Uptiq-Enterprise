package com.uptiq;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "worker")
public class WorkerProperties {

    private String regionId;
    private String workerId = "worker-1";

    private int batchSize = 100;
    private int maxConcurrency = 200;

    private long blockMs = 2000;

    private long reclaimIntervalMs = 30000;
    private long claimMinIdleMs = 60000;

    private long httpConnectTimeoutMs = 3000;
    private long httpRequestTimeoutMs = 5000;

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    public long getBlockMs() {
        return blockMs;
    }

    public void setBlockMs(long blockMs) {
        this.blockMs = blockMs;
    }

    public long getReclaimIntervalMs() {
        return reclaimIntervalMs;
    }

    public void setReclaimIntervalMs(long reclaimIntervalMs) {
        this.reclaimIntervalMs = reclaimIntervalMs;
    }

    public long getClaimMinIdleMs() {
        return claimMinIdleMs;
    }

    public void setClaimMinIdleMs(long claimMinIdleMs) {
        this.claimMinIdleMs = claimMinIdleMs;
    }

    public long getHttpConnectTimeoutMs() {
        return httpConnectTimeoutMs;
    }

    public void setHttpConnectTimeoutMs(long httpConnectTimeoutMs) {
        this.httpConnectTimeoutMs = httpConnectTimeoutMs;
    }

    public long getHttpRequestTimeoutMs() {
        return httpRequestTimeoutMs;
    }

    public void setHttpRequestTimeoutMs(long httpRequestTimeoutMs) {
        this.httpRequestTimeoutMs = httpRequestTimeoutMs;
    }
}