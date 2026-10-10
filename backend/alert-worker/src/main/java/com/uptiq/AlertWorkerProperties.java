package com.uptiq;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "alertworker")
public class AlertWorkerProperties {

    private String workerId;
    private String group;

    private int batchSize = 100;
    private long blockMs = 2000;

    private long claimMinIdleMs = 60000;
    private long reclaimIntervalMs = 30000;
    private int reclaimBatchSize = 100;

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getBlockMs() {
        return blockMs;
    }

    public void setBlockMs(long blockMs) {
        this.blockMs = blockMs;
    }

    public long getClaimMinIdleMs() {
        return claimMinIdleMs;
    }

    public void setClaimMinIdleMs(long claimMinIdleMs) {
        this.claimMinIdleMs = claimMinIdleMs;
    }

    public long getReclaimIntervalMs() {
        return reclaimIntervalMs;
    }

    public void setReclaimIntervalMs(long reclaimIntervalMs) {
        this.reclaimIntervalMs = reclaimIntervalMs;
    }

    public int getReclaimBatchSize() {
        return reclaimBatchSize;
    }

    public void setReclaimBatchSize(int reclaimBatchSize) {
        this.reclaimBatchSize = reclaimBatchSize;
    }
}