# MCSM — Minecraft Server Manager

AI 驱动的 Minecraft 服务器运维面板，面向非技术用户，通过智能体轻松管理服务器。

---

## 快速开始

### 1. 解压

将 `MCSMbyAgent.zip` 解压到任意目录。

### 2. 安装数据库

双击 `install-mysql.bat`，自动完成 MySQL 安装和数据库初始化。已预配置 root 密码 `mcsm2024`，数据库 `mcsm`。

> 如本机已有 MySQL 运行在 3306 端口，脚本会交互式引导你输入凭据并导入数据。

### 3. 启动

双击 `start.bat`，自动拉起 MySQL 和后端。浏览器打开 `http://127.0.0.1:8000`。

### 4. 安装向导

首次启动进入配置页面：

- 创建管理员账号
- 配置 LLM API（支持 DeepSeek、MiMo 等 OpenAI 兼容接口）

### 5. 创建服务器

进入「服务器管理」→「模板创建」：

- 选择 Paper 1.20.1 模板
- 填写名称，点击创建

服务器自动解压注册，启动后即可在游戏内连接。

### 6. 管理面板

| 功能 | 说明 |
|------|------|
| 服务器管理 | 启动/停止/重启，控制台，状态监控 |
| 插件管理 | 查看/启用/禁用/删除插件 |
| 玩家管理 | 在线玩家列表，背包查看 |
| 文件管理 | 配置文件浏览编辑 |
| 交易市场 | 玩家物品上架交易 |
| 内网穿透 | FRP 配置，朋友联机 |
| AI 助手 | 自然语言操控服务器 |

---

## 项目结构

```
mcsm-fixed/
  mcsm-backend/                 Spring Boot 后端
    src/main/java/              Java 源码
      agent/                    智能体核心
        ai/                     LLM 客户端、工具执行
        orchestration/          ReAct 决策循环
        communication/          WebSocket 事件流、缓存
        core/                   子 Agent（Worker/Planner/Checker）
        memory/                 知识库、备份、日志
      service/                  业务服务
      controller/               REST 控制器
      config/                   配置类
    data/                       运行时资源（开发与部署统一）
      config/                   配置模板
      templates/paper/          Paper 模板
      plugins/mcsm-bridge.jar   桥接插件
      jre/                      JDK 21
      mysql/                    MySQL 8.0 便携版 + 预初始化数据
      frp/                      FRP 客户端
      lib/mysql-connector.jar   JDBC 驱动
      init.sql                  建表脚本
      SetupCli.java             数据库安装工具
      StopCli.java              MySQL 管理工具
    start.bat                   启动
    install-mysql.bat           安装数据库
    stop-mysql.bat              停/重置 MySQL

  mcsm-fronted/                 Vue 3 前端
    src/pages/admin/            管理后台页面
    src/utils/request.js        Axios（全局 loading / silent 模式）
    src/utils/wsClient.js       STOMP WebSocket 客户端

  mcsm-minecraft-bridge/        游戏端插件
```

---

## AI 智能体架构

### 总览

智能体接收用户自然语言，在 ReAct 循环中自主决定调用哪些工具，流式输出思考过程和结果。

### 核心流程

```
用户消息 → WebSocket(STOMP)
  → AgentChatService(@Async)
    → DecisionAgent(ReAct 循环)
      → LlmClient(SSE 流式调用 LLM)
        → 解析 delta: 文本/思考/工具调用
      → ToolExecutor(执行工具)
        → 本地工具: 文件/命令/服务器
        → 后端工具: REST API 调用
      → AgentEventStream → WebSocket 推送到前端
```

### 关键组件

**DecisionAgent** — ReAct 循环（最多 8 轮），Pro 模型：
1. 发送对话历史 + 工具列表给 LLM
2. LLM 返回文本回复或工具调用
3. 工具调用 → 执行 → 结果加入历史 → 下一轮

**ToolExecutor** — 本地工具直接执行，后端工具通过 HTTP 调用

**AgentEventStream** — 流式推送事件到前端，带 seq 编号 + ACK + SessionEventBuffer 缓存，支持断线重连恢复

**LlmClient** — OpenAI 兼容 SSE 流式解析，支持 DeepSeek 和 MiMo 等厂商的 `reasoning_content` 思考字段

### 事件类型

| 事件 | 说明 |
|------|------|
| `AGENT_START` | 开始处理 |
| `THINKING` | 推理过程 |
| `REPLY_CHUNK` | 回复内容 |
| `TOOL_CALL` | 工具调用及结果 |
| `AGENT_DONE` | 处理完成 |

---

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3 + MyBatis-Plus + MySQL 8.0 |
| 前端 | Vue 3 + Element Plus + Vite |
| 通信 | STOMP WebSocket + REST API |
| AI | ReAct Agent + OpenAI 兼容 API |
| 安全 | Spring Security + JWT |
| 游戏 | Paper 1.20.1 + mcsm-bridge |

---

## 开发

```bash
# 后端
cd mcsm-backend && mvn package -DskipTests

# 前端
cd mcsm-fronted && npm install && npm run build

# 数据库工具
cd mcsm-backend/data
javac -cp "lib/mysql-connector.jar" SetupCli.java StopCli.java
```
