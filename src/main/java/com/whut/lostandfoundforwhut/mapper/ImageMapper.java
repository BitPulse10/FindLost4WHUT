package com.whut.lostandfoundforwhut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whut.lostandfoundforwhut.model.entity.Image;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author DXR
 * @date 2026/01/30
 * @description 图片 Mapper 接口
 */
public interface ImageMapper extends BaseMapper<Image> {
    
    /**
     * 根据 id 列表查询 url
     * @param ids id 列表
     * @return url 列表
     */
    @Select("<script>" +
            "SELECT url FROM images WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<String> selectUrlsByIds(List<Long> ids);
}
