# TTS 商品监控系统

## 项目概述

这是一个基于 Spring Boot 和 Vue.js 的 TTS (TikTok Shop) 商品监控系统，用于实时监控已投放商品链接的有效性，及时发现失效商品并告警，减少运营损失。

系统主要功能包括：
- 商品状态展示和管理
- 定时商品有效性校验
- 失效商品告警机制
- 支持商品的增删改查操作

## 技术栈

### 后端 (src/)
- **框架**: Spring Boot 4.0.1
- **JDK**: Java 25
- **数据库**: MySQL 8.0
- **ORM**: MyBatis Plus 3.5+
- **其他**: 线程池、限流算法、日志配置等

### 前端 (frontend/)
- **框架**: Vue.js
- **构建工具**: Vite
- **包管理**: npm

## 主要功能

### 商品监控
- 分页展示商品列表，支持按状态、国家、商品ID筛选
- 新增/删除监控商品
- 确认失效商品状态

### 校验任务
- 手动触发全量商品有效性校验
- 每日定时自动校验 (凌晨2点)
- 集成限流控制 (50 QPS) 和线程池优化

### 告警机制
- 对失效且未确认商品发送告警
- 支持飞书机器人 webhook 通知
- 详细日志记录文件便于审计

## 安装和运行

### 环境要求
- JDK 25
- MySQL 8.0
- Node.js (前端开发)

### 后端运行
1. 克隆项目到本地
2. 配置数据库连接 (application.yml)
3. 配置 TTS API 密钥和告警 webhook
4. 运行 Maven 编译: `mvn clean compile`
5. 启动应用: `mvn spring-boot:run`

### 前端运行
1. 进入 frontend 目录: `cd frontend`
2. 安装依赖: `npm install`
3. 启动开发服务器: `npm run dev`

## 项目结构

```
TTS/
├── src/                          # 后端源码
│   └── main/
│       ├── java/com/tts/monitor/ # Java 源码
│       │   ├── config/           # 配置类
│       │   ├── controller/       # REST 控制器
│       │   ├── dto/              # 数据传输对象
│       │   ├── entity/           # 实体类
│       │   ├── exception/        # 异常处理
│       │   ├── mapper/           # MyBatis 映射
│       │   ├── service/          # 业务逻辑
│       │   ├── task/             # 定时任务
│       │   └── util/             # 工具类
│       └── resources/            # 配置文件
│           ├── application.yml   # 应用配置
│           ├── db/               # 数据库脚本
│           └── mapper/           # MyBatis XML 映射
├── frontend/                     # 前端源码
│   ├── src/
│   │   ├── components/           # Vue 组件
│   │   ├── api/                  # API 调用
│   │   └── utils/                # 工具函数
│   ├── package.json              # 前端依赖
│   └── vite.config.js            # Vite 配置
├── docs/                         # 文档
│   └── TTS商品校验方案.md         # 详细方案文档
├── scripts/                      # 脚本工具
└── pom.xml                       # Maven 配置
```

## 详细文档

请参考 [TTS商品校验方案.md](TTS商品校验方案.md) 获取完整的系统设计、数据库表结构、接口文档和技术实现细节。

## 许可证

[MIT License](LICENSE)</content>
<parameter name="filePath">e:\.Code\TTS\README.md