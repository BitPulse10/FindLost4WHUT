package com.whut.lostandfoundforwhut.controller;

import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.result.Result;
import com.whut.lostandfoundforwhut.model.dto.TextEmbeddingDTO;
import com.whut.lostandfoundforwhut.service.IVectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 向量数据库控制器
 * 
 * @author Qoder
 * @date 2026/02/03
 */
@RestController
@RequestMapping("/api/vector")
@RequiredArgsConstructor
@Tag(name = "向量数据库", description = "向量数据库相关接口")
@Slf4j
public class VectorController {

    private final IVectorService vectorService;

    @PostMapping("/add-text")
    @Operation(summary = "添加文本到向量数据库", description = "将文本添加到向量数据库中进行索引")
    public Result<Void> addTextToCollection(@RequestBody TextEmbeddingDTO textEmbeddingDTO) {
        try {
            vectorService.addTextToCollection(textEmbeddingDTO);
            log.info("成功添加文本到向量数据库，ID：{}", textEmbeddingDTO.getId());
            return Result.success(null);
        } catch (Exception e) {
            log.error("添加文本到向量数据库失败，ID：{}", textEmbeddingDTO.getId(), e);
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "添加文本到向量数据库失败：" + e.getMessage());
        }
    }

    @GetMapping("/search")
    @Operation(summary = "向量搜索", description = "在向量数据库中搜索相似文本")
    public Result<List<String>> searchInCollection(
            @Parameter(description = "查询文本", required = true) @RequestParam String query,
            @Parameter(description = "返回结果数量", required = false, example = "5") @RequestParam(defaultValue = "5") int k) {
        try {
            List<String> results = vectorService.searchInCollection(query, k);
            log.info("向量搜索完成，查询：{}，返回结果数量：{}", query, results.size());
            return Result.success(results);
        } catch (Exception e) {
            log.error("向量搜索失败，查询：{}", query, e);
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "向量搜索失败：" + e.getMessage());
        }
    }

    @GetMapping("/size")
    @Operation(summary = "获取集合大小", description = "获取向量数据库集合中的条目总数")
    public Result<Integer> getCollectionSize() {
        try {
            int size = vectorService.getCollectionSize();
            log.info("获取集合大小完成，当前大小：{}", size);
            return Result.success(size);
        } catch (Exception e) {
            log.error("获取集合大小失败", e);
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "获取集合大小失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除向量数据", description = "从向量数据库中删除指定ID的文本")
    public Result<Void> deleteFromCollection(
            @Parameter(description = "要删除的文本ID", required = true) @PathVariable String id) {
        try {
            vectorService.deleteFromCollection(id);
            log.info("成功从向量数据库删除条目，ID：{}", id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("从向量数据库删除条目失败，ID：{}", id, e);
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "删除失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空向量数据库", description = "清空整个向量数据库集合")
    public Result<Void> clearCollection() {
        try {
            vectorService.clearCollection();
            log.info("成功清空向量数据库集合");
            return Result.success(null);
        } catch (Exception e) {
            log.error("清空向量数据库集合失败", e);
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "清空集合失败：" + e.getMessage());
        }
    }
}