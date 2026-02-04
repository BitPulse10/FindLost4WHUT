package com.whut.lostandfoundforwhut.controller;

import com.whut.lostandfoundforwhut.common.result.Result;
import com.whut.lostandfoundforwhut.model.entity.Tag;
import com.whut.lostandfoundforwhut.model.vo.PageResultVO;
import com.whut.lostandfoundforwhut.service.ITagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Codex
 * @date 2026/02/04
 * @description 标签控制器
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "标签管理", description = "标签相关接口")
public class TagController {

    private final ITagService tagService;

    @GetMapping
    @Operation(summary = "标签查询", description = "查询标签列表，支持关键字搜索")
    public Result<PageResultVO<Tag>> listTags(
            @Parameter(description = "关键字") @RequestParam(required = false) String q,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNo,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResultVO<Tag> result = tagService.listTags(q, pageNo, pageSize);
        return Result.success(result);
    }
}
