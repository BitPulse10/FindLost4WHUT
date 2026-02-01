# 图片服务 API 文档

## API 接口清单

### 1. 上传单个图片

- **接口地址**: `POST /api/images/upload`
- **功能描述**: 上传单个图片文件并保存到服务器
- **请求参数**:
  - 表单参数: file (MultipartFile) - 图片文件
- **返回结果**: `Result<Image>`
  - data: Image 对象，包含图片 ID 和访问 URL
  - code: 响应码
  - message: 响应消息
  - success: 是否成功
- **失败情况**:
  - 上传失败: 返回错误码 0001，消息 "文件上传失败"
  - 文件格式不支持: 返回错误码 0002 "不支持的文件类型"
  - 文件为空: 返回错误码 0002 "文件不能为空"

### 2. 批量上传图片

- **接口地址**: `POST /api/images/upload/multiple`
- **功能描述**: 批量上传多个图片文件并保存到服务器
- **请求参数**:
  - 表单参数: files (List<MultipartFile>) - 图片文件列表
- **返回结果**: `Result<List<Image>>`
  - data: Image 对象列表，每个对象包含图片 ID 和访问 URL
  - code: 响应码
  - message: 响应消息
  - success: 是否成功
- **失败情况**:
  - 某一个文件上传失败: 返回错误码 0001，消息 "文件上传失败"
  - 某一个文件格式不支持: 返回错误码 0002 "不支持的文件类型"

### 3. 根据ID获取图片信息

- **接口地址**: `GET /api/images/{id}`
- **功能描述**: 根据图片 ID 获取图片的详细信息
- **请求参数**:
  - 路径参数: id (Long) - 图片 ID
- **返回结果**: `Result<Image>`
  - data: Image 对象，包含图片 ID 和访问 URL
  - code: 响应码
  - message: 响应消息
  - success: 是否成功
- **失败情况**:
  - 查询失败: 返回错误码 0001，消息 "查询失败"
  - 图片不存在: 返回错误码 0005 "图片不存在"

### 4. 获取图片静态资源

- **接口地址**: `GET /images/**`
- **功能描述**: 根据图片相对 URL 获取图片的静态资源（直接访问图片文件）
- **请求参数**:
  - 路径参数: **relativeUrl** (String) - 图片相对 URL（例如：`/123456.jpg`）
- **返回结果**: 图片文件（根据图片格式）
- **注意**:
- **路径参数**:
  - **relativeUrl** 为图片的 url 字段

---

# 物品-图片关联服务 API 文档

## API 接口清单

### 1. 根据物品ID获取图片列表

- **接口地址**: `GET /api/item-images/item/{itemId}`
- **功能描述**: 获取指定物品关联的所有图片信息
- **请求参数**:
  - 路径参数: itemId (Long) - 物品 ID
- **返回结果**: `Result<List<Image>>`
  - data: Image 对象列表，每个对象包含图片 ID 和访问 URL
  - code: 响应码
  - message: 响应消息
  - success: 是否成功

### 2. 保存物品-图片关联关系

- **接口地址**: `POST /api/item-images/add`
- **功能描述**: 为指定物品添加图片关联关系（支持多张图片）
- **请求参数**:
  - 请求体: ItemImageAddDTO（使用蛇形命名）
    - item_id: 物品 ID (Long)
    - image_ids: 图片 ID 列表 (List<Long>)
- **返回结果**: `Result<Boolean>`
  - data: Boolean 值，表示是否保存成功
  - code: 响应码
  - message: 响应消息
  - success: 是否成功
- **失败情况**:
  - 查询失败: 返回错误码 0001，消息 "查询失败"
  - item_id 为空: 返回错误码 0002 "物品ID不能为空"

## 数据模型说明

### Image 实体类

- id: Long - 图片 ID（主键自增）
- url: String - 图片访问 URL

### ItemImage 实体类

- itemId: Long - 物品 ID（外键，关联 items 表）
- imageId: Long - 图片 ID（外键，关联 images 表）

### ItemImageAddDTO 类（使用蛇形命名）

- itemId: Long - 物品 ID
- imageIds: List<Long> - 图片 ID 列表

## 请求示例

### 上传单个图片示例

**使用 curl:**
```bash
curl -X POST "http://localhost:8080/api/images/upload" \
  -F "file=@/path/to/image.jpg"
```

**使用 Postman:**
1. 选择 POST 请求方式
2. 输入 URL: `http://localhost:8080/api/images/upload`
3. 在 Body 中选择 form-data
4. 添加 key: `file`，类型选择 File，选择要上传的图片文件
5. 点击 Send 按钮

### 批量上传图片示例

**使用 curl:**
```bash
curl -X POST "http://localhost:8080/api/images/upload/multiple" \
  -F "files=@/path/to/image1.jpg" \
  -F "files=@/path/to/image2.jpg"
```

**使用 Postman:**
1. 选择 POST 请求方式
2. 输入 URL: `http://localhost:8080/api/images/upload/multiple`
3. 在 Body 中选择 form-data
4. 添加多个 key: `files`，类型选择 File，分别选择要上传的图片文件
5. 点击 Send 按钮

### 保存物品-图片关联关系示例

**使用 curl:**
```bash
curl -X POST "http://localhost:8080/api/item-images/add" \
  -H "Content-Type: application/json" \
  -d '{
    "item_id": 1,
    "image_ids": [1, 2, 3]
  }'
```

**使用 Postman:**
1. 选择 POST 请求方式
2. 输入 URL: `http://localhost:8080/api/item-images/add`
3. 在 Headers 中添加: `Content-Type: application/json`
4. 在 Body 中选择 raw -> JSON 格式，输入上述 JSON 数据
5. 点击 Send 按钮

### 根据物品ID获取图片列表示例

**使用 curl:**
```bash
curl -X GET "http://localhost:8080/api/item-images/item/1"
```

**使用 Postman:**
1. 选择 GET 请求方式
2. 输入 URL: `http://localhost:8080/api/item-images/item/1`
3. 点击 Send 按钮

## 响应示例

### 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "url": "http://example.com/uploads/image1.jpg"
  }
}
```

### 失败响应示例

```json
{
  "code": 500,
  "message": "保存失败",
  "data": null
}
```

## 注意事项

1. **图片上传**:
   - 支持的图片格式：JPG、PNG、GIF 等常见格式
   - 图片上传后会自动生成访问 URL

2. **物品-图片关联**:
   - 保存关联关系前会自动验证物品和图片是否存在
   - 如果物品不存在，返回错误信息："物品不存在"
   - 如果图片不存在，返回错误信息："图片不存在: [图片ID]"
   - 支持为同一个物品添加多张图片

3. **蛇形命名**:
   - ItemImageAddDTO 使用蛇形命名策略
   - 请求体中的字段名必须使用下划线命名（如 `item_id`、`image_ids`）

4. **外键约束**:
   - item_images 表中的 item_id 字段有外键约束，必须引用 items 表中存在的 id
   - item_images 表中的 image_id 字段有外键约束，必须引用 images 表中存在的 id

5. **安全验证**:
   - 当前 Security 验证处于关闭状态
   - 如需开启，请修改 `application.yml` 文件中的 `app.security.enabled` 属性为 `true`

6. **图片存储路径**:
   - 图片默认存储路径：`uploads/image/`
   - 可在 `application.yml` 中通过 `app.upload.image.dir` 配置项修改存储路径
