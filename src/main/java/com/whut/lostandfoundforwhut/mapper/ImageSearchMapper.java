package com.whut.lostandfoundforwhut.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whut.lostandfoundforwhut.model.entity.ImageSearch;

public interface ImageSearchMapper extends BaseMapper<ImageSearch> {
    /**
     * 根据 id 列表查询 url
     * @param ids id 列表
     * @return url 列表
     */
    @Select("<script>" +
            "SELECT url FROM image_search WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<String> selectUrlsByIds(List<Long> ids);
    
}
