# 物品服务 API 文档

## API 接口清单

### 1. 添加物品

- **接口地址**: `POST /api/items/add-item`
- **功能描述**: 添加新的挂失或招领物品
- **请求参数**:
  - 请求体: ItemDTO
    - type: 物品类型 (0-挂失，1-招领)
    - eventTime: 事件发生时间
    - eventPlace: 事件发生地点
    - status: 物品状态 (0-有效，1-结束)
    - description: 物品描述
    - email: 用户邮箱（用于验证）
- **返回结果**: `Result<Item>`
  - data: Item 对象，包含完整的物品信息
  - code: 响应码
  - message: 响应消息
  - success: 是否成功

### 2. 更新物品

- **接口地址**: `PUT /api/items/update-item`
- **功能描述**: 通过查询参数更新物品信息
- **请求参数**:
  - 查询参数: itemId (Long) - 物品 ID
  - 请求体: ItemDTO
    - type: 物品类型 (0-挂失，1-招领)
    - eventTime: 事件发生时间
    - eventPlace: 事件发生地点
    - status: 物品状态 (0-有效，1-结束)
    - description: 物品描述
    - email: 用户邮箱
- **返回结果**: `Result<Item>`
  - data: 更新后的 Item 对象
  - code: 响应码
  - message: 响应消息
  - success: 是否成功

### 3. 下架物品

- **接口地址**: `PUT /api/items/take-down`
- **功能描述**: 通过查询参数下架物品
- **请求参数**:
  - 查询参数: itemId (Long) - 物品 ID
- **返回结果**: `Result<Boolean>`
  - data: Boolean 值，表示是否下架成功
  - code: 响应码
  - message: 响应消息
  - success: 是否成功

### 4. 筛选物品

- **接口地址**: `GET /api/items/filter`
- **功能描述**: 支持按类型、状态、关键词筛选，可单独一个条件也可以多个条件组合
- **请求参数**:
  - 查询参数: PageQueryDTO
    - pageNo: 当前页（从 1 开始，默认为 1）
    - pageSize: 每页大小（默认为 10）
  - 查询参数: type (Integer, 可选) - 物品类型 (0-挂失，1-招领)
  - 查询参数: status (Integer, 可选) - 物品状态 (0-有效，1-结束)
  - 查询参数: keyword (String, 可选) - 关键词搜索
- **返回结果**: `Result<PageResultVO<Item>>`
  - data: 分页结果对象，包含:
    - records: Item 对象列表
    - total: 总记录数
    - pageNo: 当前页
    - pageSize: 每页大小
  - code: 响应码
  - message: 响应消息
  - success: 是否成功

## 数据模型说明

### Item 实体类

- id: Long - 物品 ID（主键自增）
- userId: Long - 用户 ID
- type: Integer - 物品类型 (0-挂失，1-招领)
- eventTime: LocalDateTime - 事件发生时间
- eventPlace: String - 事件发生地点
- status: Integer - 物品状态 (0-有效，1-结束)
- isDeleted: Integer - 逻辑删除标识
- description: String - 物品描述
- itemCode: Long - 物品编号
- createdAt: LocalDateTime - 创建时间
- updatedAt: LocalDateTime - 更新时间

### 枚举类型

- **ItemType**:
  - LOST(0, "挂失")
  - FOUND(1, "招领")
- **ItemStatus**:
  - ACTIVE(0, "有效")
  - CLOSED(1, "结束")

## 安全验证配置

当前 Security 验证处于关闭状态。如需开启，请按以下步骤操作：

修改 `application.yml` 文件中的 `app.security.enabled` 属性为 `true`：

```yaml
app:
  security:
    enabled: true # 启用安全配置
```

在 Security.config 中 将 app.security.enabled:false 改为 true

## 注意事项

1. 所有 API 均需进行 JWT 身份验证（通过 Header 中的 Authorization 字段传递）
2. 目前代码中用户 ID 为硬编码（123），实际部署时需要通过 JWT 解析用户身份
3. 物品类型和状态使用数字编码，建议前端显示时转换为中文描述
4. 分页参数默认每页 10 条记录，可根据需求调整 pageSize 参数
