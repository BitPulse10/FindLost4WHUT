package com.whut.lostandfoundforwhut.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.whut.lostandfoundforwhut.model.vo.ImageSearchVO;

/**
 * 搜索图片服务接口
 */
public interface IImageSearchService {
    /**
     * 上传多个图搜临时图片
     * @param files 图搜临时图片文件列表
     * @return 图搜临时图片实体ID列表
     */
    List<ImageSearchVO> uploadImageSearchs(List<MultipartFile> files);

    /**
     * 根据图搜临时图片ID查询图搜临时图片的URL
     * @param id 图搜临时图片ID
     * @return 图搜临时图片的URL
     */
    String getUrl(Long id);

    /**
     * 根据图搜临时图片ID删除图搜临时图片
     * @param ids 图搜临时图片ID列表
     */
    void deleteImageSearchsByIds(List<Long> ids);
}
