package com.uptiq;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "dbwriter")
public class DbWriterProperties {

    private String workerId;
    private String group;

    private int batchSize = 100;
    private long blockMs = 2000;

    private long claimMinIdleMs = 60000;
    private long reclaimIntervalMs = 30000;
    private int reclaimBatchSize = 200;

    private int sslExpiringThresholdDays = 14;

    private int consensusThreshold = 1;
    private int totalRegions = 1;

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

    public int getSslExpiringThresholdDays() {
        return sslExpiringThresholdDays;
    }

    public void setSslExpiringThresholdDays(int sslExpiringThresholdDays) {
        this.sslExpiringThresholdDays = sslExpiringThresholdDays;
    }

    public int getConsensusThreshold() {
        return consensusThreshold;
    }

    public void setConsensusThreshold(int consensusThreshold) {
        this.consensusThreshold = consensusThreshold;
    }

    public int getTotalRegions() {
        return totalRegions;
    }

    public void setTotalRegions(int totalRegions) {
        this.totalRegions = totalRegions;
    }
}