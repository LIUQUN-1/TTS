# TTS商品校验方案

## 1. 背景与需求
已投放的商品链接，因为TTS的商品随时有可能失效，失效的商品对应的推广链接将不会带来任何收入且消耗正常，运营侧希望通过TTS提供的实时计划信息接口判断商品是否有效，以方便运营及时发现问题，减少损失。

需求：
1. 提供商品状态展示列表。需要展示商品的相关信息，比如商品ID,佣金信息，状态（有效/无效），确认状态（已确认/待确认）等。
2. 支持按照条件查询商品列表，筛选出符合条件的商品。比如按照国家，状态和商品ID进行筛选。
3. 支持商品的增加与删除，根据商品ID增加需要管理的商品。
4. 每天定时执行商品有效性校验任务，并支持手动执行商品有效性校验任务。对于未确认的无效商品，在i讯飞群组内告警；有效商品无需确认，无效商品确认后不再告警。

非功能性需求：
1. **安全性**：TTS API密钥和敏感信息应安全存储，避免在日志或代码中明文暴露；接口调用应使用HTTPS。
2. **可用性**：系统告警机制应可靠，确保失效商品及时通知运营。
3. **可维护性**：提供告警和确认商品失效的日志记录，便于问题排查和审计；代码应模块化，便于后续扩展。

产品原型参考：
![](./docs/image/原型.png)


## 2. 技术选型
| 组件 | 技术选型 | 用途 |
| --- | --- | --- |
| JDK | Java 25 | 运行时环境 |
| 应用框架 | Spring Boot 4.0.1 | 应用框架 |
| 数据库 | MySQL 8.0 | 持久化存储商品数据 |
| ORM | MyBatis Plus 3.5+ | 简化数据库操作 |

## 3. 数据库表设计

### 3.1 商品监控表 (`tts_product_monitor`)

该表仅存储了验证商品有效性的相关信息，未包含接口[TikTok Shop Partner Center](https://partner.tiktokshop.com/docv2/page/get-open-collaboration-product-list-by-product-ids-202509)返回的全部产品信息。

| 字段名                | 类型            | 必须 | 默认值            | 描述                                       |
| --------------------- | --------------- | ---- | ----------------- | ------------------------------------------ |
| `id`                  | INT             | 是   | AUTO_INCREMENT    | 自增主键                                   |
| `product_id`          | VARCHAR(64)     | 是   | -                 | TTS 商品唯一 ID（唯一键）                |
| `title`               | VARCHAR(255)    | 否   | ''                | 商品标题（用于列表展示）                   |
| `shop_name`           | VARCHAR(255)    | 否   | ''                | 店铺名称                                   |
| `sale_region`         | VARCHAR(10)     | 否 | NULL              | 产品销售的国家                              |
| `is_valid`            | TINYINT(1)      | 是   | 1                 | 是否有效：1-有效, 0-失效                   |
| `confirm_status`      | TINYINT(1)      | 是   | 0                 | 确认状态：0-待处理, 1-运营已确认(跳过告警) |
| `commission_rate`     | INT             | 否   | NULL              | 佣金率（万分比，如 100 代表 1%）           |
| `commission_amount`   | DECIMAL(15, 4)  | 否   | NULL              | 预估佣金金额                               |
| `commission_currency` | VARCHAR(10)         | 否   | NULL              | 佣金货币单位 (如 USD, IDR)                 |
| `last_check_time`     | DATETIME        | 否   | NULL              | 最后一次系统校验的时间                 |
| `created_at`          | TIMESTAMP       | 是   | CURRENT_TIMESTAMP | 记录创建时间                               |
| `updated_at`          | TIMESTAMP       | 是   | CURRENT_TIMESTAMP | 记录更新时间                               |

**建表语句**

```sql
CREATE TABLE `tts_product_monitor` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `product_id` varchar(64) NOT NULL COMMENT 'TTS 商品唯一 ID',
  `title` varchar(255) DEFAULT '' COMMENT '商品标题',
  `shop_name` varchar(255) DEFAULT '' COMMENT '店铺名称',
  `sale_region` varchar(10) DEFAULT NULL COMMENT '产品销售的国家',
  `is_valid` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效, 0-失效',
  `confirm_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '确认状态：0-待处理, 1-运营已确认(跳过告警)',
  `commission_rate` int DEFAULT NULL COMMENT '佣金率（万分比，如 100 代表 1%）',
  `commission_amount` decimal(15,4) DEFAULT NULL COMMENT '预估佣金金额',
  `commission_currency` varchar(10) DEFAULT NULL COMMENT '佣金货币单位 (如 USD, IDR)',
  `last_check_time` datetime DEFAULT NULL COMMENT '最后一次系统校验的时间',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_id` (`product_id`)
)
```
## 4. 商品校验功能

![](./docs/image/未命名绘图.png)

**限流**:根据官方文档[TikTok Shop Partner Center](https://partner.tiktokshop.com/docv2/page/rate-limits)说明，每个应用对**单个 API**的默认限流阈值为 **50 QPS（每秒查询次数）**，所有请求无论返回数据量大小，均按 1:1 的比例计入请求次数统计。

- **功能描述**: 校验 `tts_product_monitor` 表中所有商品的有效性，通过调用第三方TTS接口查询商品信息（从 YAML 配置文件加载 TTS 配置）。手动触发和定时任务都会使用此功能。
- **校验逻辑**:
  1. 分页查询 `tts_product_monitor` 表中所有商品的 `product_id`，每次取1000条，处理完一批再取下一批，从源头控制流量，避免一次性加载过多数据导致内存压力。
  2. 将商品ID列表分批处理（假设单次请求50个商品ID），避免单次请求过大，导致响应速率过慢或者ID被截断。
  3. 使用线程池并行发起每批请求，线程池采用有界队列，阻塞提交策略，当工作队列满时，生产者（拉取数据的线程）必须阻塞等待，直到队列有空余空间。
  4. 控制请求频率，使用令牌桶算法，每秒固定补充令牌至50个，确保每秒不超过50次请求（QPS限制）。
  5. 线程池中的Worker线程在执行实际网络请求前，必须先获取令牌。若令牌不足，Worker线程会阻塞在获取令牌的步骤。
  6. 从 YAML 配置文件加载 TTS 配置信息，对每批商品ID，调用第三方TTS接口（Get Open Collaboration Product List By Product Ids），将商品ID列表以逗号分隔拼接到URL查询参数 `product_ids` 中。
  7. 根据接口响应更新商品状态：
     - 如果接口返回商品数据且 `commission` 字段不为空，则更新 `is_valid = 1`（有效）。
     - 如果 `commission` 字段为空，则更新 `is_valid = 0`（失效）。
  8. 更新所有商品的 `last_check_time` 为当前时间。
- **触发方式**: 
  - 手动：通过接口 `/TTS/monitor/task/execute` 触发。
  - 定时：每日定时任务自动执行。

## 5. 后端接口设计
### 5.1 获取商品监控列表

- **URL**: `/TTS/monitor/products` **Method**: `GET`

- **功能描述**: 分页获取监控商品列表，支持按状态筛选。

- **请求参数**:
  | 参数名 | 类型 | 必填 | 说明 |
  | --- | --- | --- | --- |
  | page | int | 否 | 页码，默认1 |
  | size | int | 否 | 每页条数，默认20 |
  | is_valid | int | 否 | 状态筛选：1-有效，0-失效 |
  | sale_region | string | 否 | 销售国家筛选，如 IDR |
  | keyword | string | 否 | 搜索关键词（商品ID） |
- **响应参数**:
  | 参数名 | 类型 | 说明 |
  | --- | --- | --- |
  | code | int | 响应码，200表示成功，非200表示失败 |
  | message | string | 响应消息描述 |
  | data | object | 响应数据对象 |
  | data.page | int | 当前页码 |
  | data.size | int | 每页条数 |
  | data.total | int | 总记录数 |
  | data.records | array<object> | 商品记录列表，每个对象包含`tts_product_monitor`表中的全部字段：id, product_id, title, shop_name, sale_region, is_valid, confirm_status, commission_rate, commission_amount, commission_currency, last_check_time |

- **实现流程**：
  1. 解析请求参数，设置默认值（page=1, size=20）。
  2. 构建查询条件：根据提供的参数构造多条件查询（所有条件均为可选），如果提供了is_valid，则筛选商品状态；如果提供了sale_region，则筛选销售地区；如果提供了keyword，则模糊匹配product_id。
  3. 使用MyBatis Plus的Page对象执行分页查询。
  4. 将查询结果封装为分页响应对象，返回给前端。


### 5.2 新增监控商品

- **URL**: `/TTS/monitor/products/add`  **Method**: `POST`
- **功能描述**: 批量新增监控商品ID。
- **请求参数**:
  | 参数名 | 类型 | 必填 | 说明 |
  | --- | --- | --- | --- |
  | product_ids | array<string> | 是 | 商品ID列表，批量新增的TTS商品唯一ID |

- **响应参数**:
  | 参数名 | 类型 | 说明 |
  | --- | --- | --- |
  | code | int | 响应码，200表示成功，非200表示失败 |
  | message | string | 响应消息描述 |

- **实现流程**：
  1. 对请求中的 `product_ids` 数组进行去重，避免批量请求中包含重复ID导致数据库唯一键冲突。
  2. 从 YAML 配置文件加载 TTS 配置信息，获取app_key, app_secret和access_token等信息，调用[第三方TTS接口](#8-第三方tts接口信息)查询商品ID的相关信息。
  3. 如果成功返回相关信息，说明是合法的商品ID，将商品ID及查询得到的相关信息入库。
  4. 如果第三方TTS接口返回的data字段为空值，说明商品ID错误，不需要入库。如果调用第三方TTS接口失败，返回相关报错信息。

### 5.3 删除监控商品

- **URL**: `/TTS/monitor/products/{product_id}` **Method**: `DELETE`
- **功能描述**: 删除指定商品ID的监控商品，无效且未确认的商品不允许删除。
- **响应参数**:
  | 参数名 | 类型 | 说明 |
  | --- | --- | --- |
  | code | int | 响应码，200表示成功，非200表示失败 |
  | message | string | 响应消息描述 |
- **实现流程**：
  1. 根据路径参数product_id查询数据库中的商品记录。
  2. 检查商品的is_valid和confirm_status：如果is_valid=0（失效）且confirm_status=0（待处理），则返回错误信息，不允许删除。
  3. 否则，执行删除操作。
  4. 返回删除结果。

### 5.4 确认商品失效

- **URL**: `/TTS/monitor/products/{product_id}/confirm` **Method**: `POST`
- **功能描述**: 将指定商品的确认状态从待处理（0）更新为已确认（1），表示运营已确认该商品失效，跳过后续告警。
- **响应参数**:
  | 参数名 | 类型 | 说明 |
  | --- | --- | --- |
  | code | int | 响应码，200表示成功，非200表示失败 |
  | message | string | 响应消息描述 |
- **实现流程**：
  1. 根据路径参数 product_id 查询数据库中的商品记录。
  2. 检查商品的 is_valid：如果 is_valid=1（有效），返回错误信息，不允许确认（有效商品无需确认）。
  3. 如果 is_valid=0（失效），更新 confirm_status 为 1。
  4. 返回更新结果。

### 5.5 手动触发校验任务

- **URL**: `/TTS/monitor/task/execute` **Method**: `POST`
- **功能描述**: 立即触发一次全量商品有效性校验任务。
- **实现流程**：
  1. 接收POST请求，无需请求体。
  2. 调用商品校验功能，执行全量商品有效性校验（参考第4节校验逻辑）。
  3. 校验完成后，返回执行状态和结果。

### 5.6 定时校验任务

- **功能描述**: 每日定时执行全量商品有效性校验任务。

- **实现方式**: 使用Spring Boot的@Scheduled注解，配置cron表达式为"0 0 2 * * ?"（每天凌晨2点）。

- **实现流程**:
  1. 定时任务触发时，调用商品校验功能（参考第4节校验逻辑）。
  2. 校验完成后，触发告警机制（参考第6节）。
  
     
## 6. 告警机制设计

- **触发时机**：每日定时校验任务完成后，或手动触发校验任务后。
- **过滤条件**：仅针对 `is_valid = 0` (失效) 且 `confirm_status = 0` (待处理) 的商品发送告警。已确认的失效商品将跳过告警。
- **发送频率**：对于失效且未确认的商品，每天至少告警一次。

系统通过 i讯飞/飞书 机器人 Webhook 在群组内发送卡片或文本消息。

#### 调用示例 (cURL)

```bash
curl -X POST -H "Content-Type: application/json" \
-d '{"msg_type":"text","content":{"text":"【TTS 商品失效告警】\n商品 ID: 1729432087292775344 校验失效，请核查。"}}' \
https://open.feishu.cn/open-apis/bot/v2/hook/****
```

### 日志配置

配置专门的日志记录告警信息到文件中，便于后续审计和分析。

- **日志框架**：使用Spring Boot默认的Logback。
- **日志格式**：`%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n`
- **日志文件配置**：配置按日期滚动的日志文件，每天生成一个新文件，文件名为 `tts-alert-YYYY-MM-DD.log`，保留30天的历史文件。
- **使用方式**：在告警发送和运营确认代码中使用专用logger记录，如：
  - 告警：`logger.info("告警发送：商品ID={}, 消息={}", productId, message);`
  - 确认：`logger.info("运营确认：商品ID={}, 操作=确认失效", productId);`



## 7. 第三方TTS接口信息

该接口用于通过指定的商品 ID 列表，获取相关公开协作（Open Collaboration）商品的详细信息。

### 7.1 基础信息

- **接口名称**：Get Open Collaboration Product List By Product Ids
- **请求方法**：`POST`
- **请求路径**：`/affiliate_creator/202509/open_collaborations/products`
- **所需权限范围 (Scope)**：`creator.affiliate_collaboration.read`
- **接口版本**：202509

### 7.2 调用示例

#### 请求示例 (cURL)

```
# Header参数：
# content-type: application/json
# x-tts-access-token: Creator 访问令牌
# Query参数：
# app_key: 应用唯一 Key
# sign: 请求签名，通过TTS官方算法(https://partner.tiktokshop.com/docv2/page/sign-your-api-request)生成
# timestamp: Unix 时间戳 (GMT/UTC+0)
# product_ids: 逗号分隔的商品 ID 列表，例如 123,456
curl -X POST \
 'https://open-api.tiktokglobalshop.com/affiliate_creator/202509/open_collaborations/products?app_key=38abcd&sign=5361235029d141222525e303d742f9e38aea052d10896d3197ab9d6233730b8c&timestamp=1623812664&product_ids=123,456' \
-H 'x-tts-access-token: TTP_pwSm2AAAAABmmtFz1xlyKMnwg74T2GJ5s0uQbS8jPjb_GkdFVCxPqzQXSyuyfXdQa0AqyDsea2tYFNVf4XeqgZHFfPyv0Vs659QqyLYfsGzanZ5XZAin3_ZkcIxxS0_In6u6XDeU96k' \
-H 'content-type: application/json' \
-d '{}'
```

#### 响应示例 (JSON)

```
{
  "code": 0, // 错误码。0 表示请求成功，非 0 表示请求失败。
  "data": {
    "products": [ 
      {
        "shop": {
          "name": "Test shop" // 店铺名称。
        },
        "id": "1729432087292775344", // 商品 ID。
        "has_inventory": false, // 是否有库存。
        "units_sold": 12, // 销量。
        "title": "Blue t-shirt", // 商品标题。
        "sale_region": "ID", // 销售地区。商品销售的地域代码（。
        "main_image_url": "https://p16-oec-va.ibyteimg.com/...", // 商品主图链接。
        "detail_link": "https://shop.tiktok.com/view/product/...", // 商品详情页链接。
        "original_price": { // 商品原价。通常指划线价或打折前的价格。
          "currency": "USD", // 在"sale_region"的货币。
          "minimum_amount": "12.21", // 产品SKU的最小原价。
          "maximum_amount": "100.00" // 产品SKU的最大原价。
        },
        "category_chains": [ // 类目链。描述商品所属的层级分类。
          {
            "id": "3435545", // 类目 ID。
            "local_name": "COMPUTER", // 本层类目名称。
            "is_leaf": false, // 是否为叶子类目。
            "parent_id": "12345" // 父类目 ID。
          }
        ],
        "commission": { // 佣金详情。
          "rate": 100, // 佣金率。以万分比表示（如 100 代表 1%），区间是 [100, 8000]。
          "currency": "USD", // 佣金货币单位。
          "amount": "123" // 预估佣金金额。
        },
        "sales_price": { // 销售价格。商品当前实际售卖的价格区间。
          "currency": "USD", // 货币单位。
          "minimum_amount": "34.3", // 产品SKU的最小销售价。
          "maximum_amount": "55.7" // 产品SKU的最大销售价。
        },
        "shop_ads_commission": { // 店铺广告佣金（针对通过广告带来的成交）。
          "rate": 100 // 广告佣金率（万分比）。
        }
      }
    ]
  },
  "message": "Success", // 响应消息描述。
  "request_id": "202203070749000101890810281E8C70B7" // 请求唯一 ID。用于链路追踪和问题排查。
}
```

