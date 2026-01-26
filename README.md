# TTS Product Monitor

TTS商品校验监控系统 - Spring Boot 3.4.4

## 项目结构

```
src/
├── main/
│   ├── java/com/tts/monitor/
│   │   ├── TtsMonitorApplication.java      # 主启动类
│   │   ├── config/                          # 配置类
│   │   ├── controller/                      # REST控制器
│   │   ├── service/                         # 业务服务层
│   │   ├── mapper/                          # MyBatis Mapper
│   │   ├── entity/                          # 实体类
│   │   ├── dto/                             # 数据传输对象
│   │   ├── util/                            # 工具类
│   │   └── task/                            # 定时任务
│   └── resources/
│       ├── application.yml                  # 应用配置
│       ├── logback-spring.xml               # 日志配置
│       ├── mapper/                          # MyBatis XML映射
│       └── db/
│           └── schema.sql                   # 数据库建表脚本
└── test/                                    # 测试代码
```

## 快速开始

### 1. 数据库初始化

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

### 2. 修改配置

编辑 `src/main/resources/application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tts_monitor
    username: your_username
    password: your_password
```

### 3. 编译运行

```bash
mvn clean package
java -jar target/tts-monitor-1.0.0.jar
```

或使用Maven直接运行：

```bash
mvn spring-boot:run
```

## 主要功能

- ✅ 商品监控列表查询（分页+筛选）
- ✅ 批量新增监控商品
- ✅ 删除监控商品
- ✅ 确认商品失效
- ✅ 手动触发校验任务
- ✅ 定时校验任务（每天凌晨2点）
- ✅ 飞书告警通知

## 技术栈

- Java 17
- Spring Boot 3.4.4
- MyBatis Plus 3.5.9
- MySQL 8.0
- OkHttp 4.12.0
- Guava (令牌桶限流)
