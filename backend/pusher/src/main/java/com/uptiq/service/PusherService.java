package com.uptiq.service;

import com.uptiq.redis.RedisMessageFields;
import com.uptiq.redis.RedisStreamConstants;
import com.uptiq.redis.RedisStreamService;
import com.uptiq.repository.WebsiteRepository;
import com.uptiq.repository.projection.WebsiteCheckTarget;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PusherService {

    private static final int BATCH_SIZE = 500;

    private final WebsiteRepository websiteRepository;
    private final RedisStreamService redisStreamService;

    public PusherService(WebsiteRepository websiteRepository, RedisStreamService redisStreamService) {
        this.websiteRepository = websiteRepository;
        this.redisStreamService = redisStreamService;
    }

    public int pushWebsiteChecks() {
        int totalScheduled = 0;
        PageRequest pageable = PageRequest.of(
                0,
                BATCH_SIZE,
                Sort.by(
                        Sort.Direction.ASC,
                        "id"
                )
        );

        Slice<WebsiteCheckTarget> slice;
        do {
            slice = websiteRepository.findByIsActiveTrue(pageable);
            List<Map<String, String>> messages = new ArrayList<>(slice.getNumberOfElements());
            for (WebsiteCheckTarget website : slice) {
                messages.add(
                        Map.of(
                                RedisMessageFields.WEBSITE_ID,
                                website.getId().toString(),

                                RedisMessageFields.URL,
                                website.getUrl()
                        )
                );
            }

            redisStreamService.xAddBulk(RedisStreamConstants.CHECK_STREAM, messages);
            totalScheduled += messages.size();
            pageable = PageRequest.of(
                    slice.getNumber() + 1,
                    BATCH_SIZE,
                    Sort.by(
                            Sort.Direction.ASC,
                            "id"
                    )
            );

        } while (slice.hasNext());
        return totalScheduled;
    }
}