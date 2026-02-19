package com.whut.lostandfoundforwhut.service.impl;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import java.time.Duration;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.lostandfoundforwhut.common.constant.Constants.RedisKey;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.common.utils.cos.COS;
import com.whut.lostandfoundforwhut.common.utils.cos.ContentReviewer;
import com.whut.lostandfoundforwhut.common.utils.cos.ImageProcessor;
import com.whut.lostandfoundforwhut.common.utils.image.ImageValidator;
import com.whut.lostandfoundforwhut.mapper.ImageMapper;
import com.whut.lostandfoundforwhut.mapper.ImageSearchMapper;
import com.whut.lostandfoundforwhut.model.entity.Image;
import com.whut.lostandfoundforwhut.model.entity.ImageSearch;
import com.whut.lostandfoundforwhut.model.vo.ImageSearchVO;
import com.whut.lostandfoundforwhut.service.IImageSearchService;
import com.whut.lostandfoundforwhut.service.IRedisService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageSearchServiceImpl extends ServiceImpl<ImageMapper, Image> implements IImageSearchService {
    @Value("${app.upload.max-file-size}")
    private String maxFileSize; // 最大文件大小
    private Long maxFileSizeBytes; // 最大文件大小（字节）
    
    private String IMAGE_SEARCH_EXPIRE_TIME = "1h"; // 图片搜索过期时间
    private Duration imageSearchExpireDuration;
    
    private String IMAGE_SEARCH_OBJECT_KEY_PREFIX = "image-search/"; // 图片搜索对象键前缀
    private List<String> allowedExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif");

    private final COS cos; // COS客户端
    private final ContentReviewer contentReviewer; // 内容审核器
    private final ImageProcessor imageProcessor; // 图片处理器

    private final IRedisService redisService;
    private final ImageSearchMapper imageSearchMapper;

    @PostConstruct
    public void init() {
        this.maxFileSizeBytes = DataSize.parse(maxFileSize).toBytes();
        this.imageSearchExpireDuration = Duration.parse("PT" + IMAGE_SEARCH_EXPIRE_TIME.toUpperCase());
    }

    /**
     * 上传图搜临时图片文件列表
     * @param files 图搜临时图片文件列表
     * @return 图搜临时图片实体ID列表
     */
    @Override
    public List<ImageSearchVO> uploadImageSearchs(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) { return new ArrayList<>(); }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) { continue; }
            validateImageFile(file);
        }

        List<String> objectKeys = new ArrayList<>(); // COS 存储对象键列表
        List<ImageSearch> imageSearchs = new ArrayList<>(); // 图片实体列表
        try {
            // 上传所有文件
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) { continue; }
                String objectKey = uploadFileToCOSReturnObjectKey(file, IMAGE_SEARCH_OBJECT_KEY_PREFIX);
                objectKeys.add(objectKey);
            }

            // 审核所有图片
            List<String> messages = contentReviewer.batchReviewImageKey(objectKeys);
            String message = joinMessages(messages, "; ", (i, msg) -> "图片" + (i + 1) + "审核失败: " + msg);
            if (message != null) { throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), message); }
            // 压缩所有图片
            for (String objectKey : objectKeys) { imageProcessor.processimage(objectKey); }
            // 设置所有图片为公共读权限
            for (String objectKey : objectKeys) { cos.setObjectPublicRead(objectKey); }

            // 批量保存到数据库
            for (String objectKey : objectKeys) {
                // 创建图片对象
                ImageSearch imageSearch = new ImageSearch();
                imageSearch.setObjectKey(objectKey);
                imageSearch.setUrl(cos.getObjectUrl(objectKey));
                imageSearch.setCreateTime(LocalDateTime.now());
                imageSearch.setExpireTime(LocalDateTime.now().plus(imageSearchExpireDuration));
                // 图片对象保存到列表
                imageSearchs.add(imageSearch);
            }
            imageSearchMapper.insert(imageSearchs);

            // 保存到数据库
            for (ImageSearch imageSearch : imageSearchs) {
                String cacheKey = RedisKey.IMAGE_SEARCH_BY_ID + imageSearch.getId();
                redisService.setValue(cacheKey, imageSearch);
            }

            // 缓存所有图片
            for (ImageSearch imageSearch : imageSearchs) {
                String cacheKey = RedisKey.IMAGE_SEARCH_BY_ID + imageSearch.getId();
                redisService.setValue(cacheKey, imageSearch, imageSearchExpireDuration);
            }

            log.info("[ImageSearchServiceImpl] 已上传 {} 张图片搜索", imageSearchs.size());
            return imageSearchs.stream().
                    map(imageSearch -> new ImageSearchVO(imageSearch.getId(), imageSearch.getUrl())).
                    collect(Collectors.toList());
        } catch (Exception e) {
            // 删除COS上的文件
            cos.batchDeleteObject(objectKeys);

            // 处理异常
            if (e instanceof AppException) throw (AppException) e;
            throw new AppException(ResponseCode.UN_ERROR.getCode(), "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * @description 根据图搜临时图片ID删除图搜临时图片
     * @param ids 图搜临时图片ID列表
     */
    @Override
    public void deleteImageSearchsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {  return; }

        List<ImageSearch> imageSearchs = imageSearchMapper.selectByIds(ids);
        // 删除数据库记录
        imageSearchMapper.deleteByIds(ids);
        // 删除COS上的文件
        List<String> objectKeys = imageSearchs.stream().map(ImageSearch::getObjectKey).collect(Collectors.toList());
        cos.batchDeleteObject(objectKeys);
        // 删除所有图片缓存
        for (Long id : ids) {
            String cacheKey = RedisKey.IMAGE_SEARCH_BY_ID + id;
            if (redisService.isExists(cacheKey)) {
                redisService.remove(cacheKey);
            }
        }
        
        log.info("[ImageSearchServiceImpl] 已删除 {} 条图片搜索记录和 {} 个COS文件", ids.size(), objectKeys.size());
    }

    /**
     * @description 获取图片搜索URL
     * @param id 图片搜索ID
     * @return 图片搜索URL
     */
    @Override
    public String getUrl(Long id) {
        // 从缓存中获取图片URL
        String cacheKey = RedisKey.IMAGE_SEARCH_BY_ID + id;
        if (redisService.isExists(cacheKey)) {
            ImageSearch imageSearch = (ImageSearch) redisService.getValue(cacheKey);
            return imageSearch != null ? imageSearch.getUrl() : null; // 缓存空值，说明不存在
        }

        // 从数据库中获取图片
        ImageSearch imageSearch = imageSearchMapper.selectById(id);
        if (imageSearch == null) {
            // 数据库也没有，缓存空值
            redisService.setValue(cacheKey, null, Duration.ofHours(1));
            throw new AppException(ResponseCode.RESOURCE_NOT_FOUND.getCode(), "图片不存在");
        }
        // 缓存图片
        redisService.setValue(cacheKey, imageSearch, imageSearchExpireDuration);
        return imageSearch.getUrl();
    }

    /**
     * @description 根据过期时间查询过期的图片搜索
     * @param expireTime 过期时间
     * @return 过期的图片搜索列表
     */
    @Override
    public List<ImageSearch> findExpiredBefore(LocalDateTime expireTime) {
        return imageSearchMapper.selectExpiredBefore(expireTime);
    }

    // 验证文件
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "文件不能为空");
        }
        // 验证文件是否为图片
        String errorMessage = ImageValidator.validateImageFile(file, allowedExtensions);
        if (errorMessage != null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), errorMessage);
        }
        // 验证图片大小
        errorMessage = ImageValidator.validateImageSize(file, maxFileSizeBytes);
        if (errorMessage != null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), errorMessage);
        }
    }

    // 生成文件名
    private String generateFileName(String extension) {
        return System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + extension;
    }

    //上传文件到COS并返回对象键
    private String uploadFileToCOSReturnObjectKey(MultipartFile file, String prefix) throws IOException {
        // 获取文件扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        // 生成对象键
        String objectKey = prefix + generateFileName(extension);

        // 先保存到临时文件
        File tempFile = File.createTempFile("temp", extension);
        file.transferTo(tempFile.toPath());
        // 上传到COS
        cos.uploadFile(tempFile, objectKey);
        tempFile.delete();
        // 返回对象键
        return objectKey;
    }

    // 合并消息列表
    private String joinMessages(
            List<String> messages,
            String separator,
            BiFunction<Integer, String, String> mapper) {
        if (messages == null || messages.isEmpty()) { return null; }

        if (separator == null) { separator = ", "; }
        if (mapper == null) { mapper = (index, message) -> message; }

        List<String> messageItems = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) == null) { continue; }

            String messageItem = mapper.apply(i, messages.get(i));
            messageItems.add(messageItem);
        }
        if (messageItems.isEmpty()) { return null; }
        return messageItems.stream().collect(Collectors.joining(separator));
    }
}
