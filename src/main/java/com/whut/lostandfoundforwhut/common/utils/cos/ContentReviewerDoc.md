# ContentReviewer 类文档

## 1. 功能描述

`ContentReviewer` 是一个内容审核工具类，用于检测内容中是否包含违规内容（如色情、广告、违法等）。

## 2. 核心方法

### 2.1 reviewText 方法

```java
public String reviewText(String text) 
```

**方法描述**：
- 审核文本内容，判断是否包含违规内容，违规内容类型包括色情、广告、违法、谩骂、政治和暴恐
- 若文本为空或 null，则直接返回空字符串

**参数说明**：
- `text`：需要审核的文本内容

**返回值说明**：
- `""`（空字符串）：审核通过，文本内容合法
- `"包含{label}内容"`：包含违规内容, {label} 为违规内容类型
- `"疑似{label}内容"`：疑似包含违规内容, {label} 为违规内容类型
- `"审核失败: {message}"`：审核过程中出现异常，{message} 为异常信息

## 4. 使用示例

### 4.1 基本使用

```java
// 注入 ContentReviewer 实例
@Autowired
private ContentReviewer contentReviewer;
```
