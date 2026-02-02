package com.whut.lostandfoundforwhut.controller;

import com.whut.lostandfoundforwhut.common.result.Result;
import com.whut.lostandfoundforwhut.model.dto.TextEmbeddingDTO;
import com.whut.lostandfoundforwhut.service.IVectorService;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Qoder
 * @date 2026/02/02
 * @description 向量数据库控制器
 */
@RestController
@RequestMapping("/api/vector")
@RequiredArgsConstructor
@Tag(name = "向量数据库管理", description = "向量数据库相关接口")
public class VectorController {

    private final IVectorService vectorService;

    @PostMapping("/add-text")
    @Operation(summary = "添加文本到向量库", description = "将文本内容添加到向量数据库中")
    public Result<Boolean> addTextToCollection(@RequestBody TextEmbeddingDTO textEmbeddingDTO) {
        try {
            vectorService.addTextToCollection(textEmbeddingDTO);
            System.out.println("文本信息已添加到向量数据库，ID：" + textEmbeddingDTO.getId());
            return Result.success(true);
        } catch (Exception e) {
            System.out.println("添加到向量数据库时发生异常：" + e.getMessage());
            e.printStackTrace();
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "添加文本到向量数据库失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "从向量库删除文本", description = "根据ID从向量数据库中删除文本内容")
    public Result<Boolean> deleteFromCollection(@PathVariable String id) {
        try {
            vectorService.deleteFromCollection(id);
            System.out.println("向量数据库中条目已删除，ID：" + id);
            return Result.success(true);
        } catch (Exception e) {
            System.out.println("删除向量数据库条目时发生异常：" + e.getMessage());
            e.printStackTrace();
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "从向量数据库删除条目失败：" + e.getMessage());
        }
    }

    @PostMapping("/search")
    @Operation(summary = "向量搜索", description = "通过文本描述搜索相似的内容")
    public Result<List<String>> searchInCollection(
            @RequestBody String query,
            @RequestParam(defaultValue = "5") int maxResults) {
        try {
            List<String> results = vectorService.searchInCollection(query, maxResults);
            return Result.success(results);
        } catch (Exception e) {
            System.out.println("向量搜索时发生异常：" + e.getMessage());
            e.printStackTrace();
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "向量搜索失败：" + e.getMessage());
        }
    }

    @GetMapping("/size")
    @Operation(summary = "获取向量数据库大小", description = "获取向量数据库中存储的条目数量")
    public Result<Integer> getCollectionSize() {
        try {
            int size = vectorService.getCollectionSize();
            return Result.success(size);
        } catch (Exception e) {
            System.out.println("获取向量数据库大小时发生异常：" + e.getMessage());
            e.printStackTrace();
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "获取向量数据库大小失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空向量数据库", description = "清空向量数据库中的所有条目")
    public Result<Boolean> clearCollection() {
        try {
            vectorService.clearCollection();
            System.out.println("向量数据库已清空");
            return Result.success(true);
        } catch (Exception e) {
            System.out.println("清空向量数据库时发生异常：" + e.getMessage());
            e.printStackTrace();
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "清空向量数据库失败：" + e.getMessage());
        }
    }

    @PutMapping("/update-text")
    @Operation(summary = "更新向量库中的文本", description = "更新向量数据库中指定ID的文本内容")
    public Result<Boolean> updateTextInCollection(@RequestBody TextEmbeddingDTO textEmbeddingDTO) {
        try {
            // 先删除旧的条目
            vectorService.deleteFromCollection(textEmbeddingDTO.getId());

            // 再添加新的条目
            vectorService.addTextToCollection(textEmbeddingDTO);
            System.out.println("向量数据库中条目已更新，ID：" + textEmbeddingDTO.getId());
            return Result.success(true);
        } catch (Exception e) {
            System.out.println("更新向量数据库条目时发生异常：" + e.getMessage());
            e.printStackTrace();
            return Result.fail(ResponseCode.UN_ERROR.getCode(), "更新向量数据库条目失败：" + e.getMessage());
        }
    }
}