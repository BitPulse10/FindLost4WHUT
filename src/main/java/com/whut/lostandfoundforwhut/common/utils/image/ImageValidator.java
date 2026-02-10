package com.whut.lostandfoundforwhut.common.utils.image;

import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * @author DXR
 * @date 2026/01/31
 * @description 图片文件验证工具类
 */
public class ImageValidator {
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
        ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"
    );

    // private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
    //     "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp"
    // );

    /**
     * 验证文件是否为图片
     * @param file 上传的文件
     * @param extensions 允许的文件扩展名列表
     * @return 错误信息，如果验证通过则返回 null
     */
    public static String validateImageFile(MultipartFile file, List<String> extensions) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "文件名不能为空";
        }

        // 检查文件扩展名
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!extensions.contains(extension)) {
            return "不支持的文件类型";
        }

        return null;
    }

    /**
     * 验证文件是否为图片
     * @param file 上传的文件
     * @return 错误信息，如果验证通过则返回 null
     */
    public static String validateImageFile(MultipartFile file) {
        return validateImageFile(file, ALLOWED_IMAGE_EXTENSIONS);
    }

    /**
     * 验证图片大小
     * @param file 上传的文件
     * @param maxSize 最大图片大小限制（字节）
     * @return 错误信息，如果验证通过则返回 null
     */
    public static String validateImageSize(MultipartFile file, long maxSize) {
        if (file.getSize() > maxSize) {
            return "图片大小不能超过" + (maxSize / (1024 * 1024)) + "MB";
        }
        return null;
    }

    /**
     * 获取允许的图片扩展名列表
     * @return 扩展名列表
     */
    public static List<String> getAllowedExtensions() { return ALLOWED_IMAGE_EXTENSIONS; }
}
