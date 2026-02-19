package com.whut.lostandfoundforwhut.job;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.whut.lostandfoundforwhut.model.entity.ImageSearch;
import com.whut.lostandfoundforwhut.service.IImageSearchService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ClearImageSearchTask {

    @Autowired
    private IImageSearchService imageSearchService;

    @Scheduled(fixedDelayString = "${scheduling.task.clear-image-search.interval}")
    public void executeClearImageSearch() {
        try {
            log.info("[ClearImageSearchTask] 开始清除过期图搜临时图片");

            List<ImageSearch> expiredImageSearches = imageSearchService.findExpiredBefore(LocalDateTime.now());
            List<Long> expiredImageSearchIds = expiredImageSearches.stream().map(ImageSearch::getId).toList();

            imageSearchService.deleteImageSearchsByIds(expiredImageSearchIds);
        } catch (Exception e) {
            log.error("清除过期图搜临时图片失败", e);
        }
    }
}
