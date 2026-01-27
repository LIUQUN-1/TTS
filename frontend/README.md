# TTS商品监控管理系统 - 前端

基于 Vue 3 + Element Plus 的TTS商品监控管理界面。

## 技术栈

- Vue 3
- Element Plus
- Axios
- Vite

## 功能特性

- ✅ 商品列表查询（分页、筛选）
- ✅ 批量新增监控商品
- ✅ 删除监控商品
- ✅ 确认商品失效状态
- ✅ 手动触发校验任务
- ✅ 商品总数统计

## 快速开始

### 1. 安装依赖

```bash
cd frontend
npm install
```

### 2. 启动后端服务

确保后端服务已在 `http://localhost:8080` 运行。

### 3. 启动开发服务器

```bash
npm run dev
```

访问：http://localhost:3000

### 4. 构建生产版本

```bash
npm run build
```

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API接口定义
│   │   └── index.js
│   ├── components/       # Vue组件
│   │   └── ProductList.vue
│   ├── utils/            # 工具函数
│   │   └── request.js    # Axios封装
│   ├── App.vue           # 根组件
│   └── main.js           # 入口文件
├── index.html
├── vite.config.js        # Vite配置（含代理）
└── package.json
```

## API接口

### 商品管理
- `GET /TTS/monitor/products` - 获取商品列表
- `POST /TTS/monitor/products` - 批量新增商品
- `DELETE /TTS/monitor/products/{productId}` - 删除商品
- `POST /TTS/monitor/products/{productId}/confirm` - 确认失效
- `GET /TTS/monitor/products/count` - 商品总数

### 任务控制
- `POST /TTS/monitor/task/execute` - 手动触发校验

## 开发说明

1. **代理配置**：Vite已配置代理，将 `/TTS` 请求转发到 `http://localhost:8080`
2. **跨域处理**：后端已添加CORS配置支持前端访问
3. **状态码**：后端统一返回格式 `{ code: 200, message: '', data: {} }`

## 注意事项

- 确保后端服务先启动
- 商品ID支持多种输入格式（换行、逗号、空格分隔）
- 失效且未确认的商品不允许删除（后端业务规则）
