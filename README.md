# java-ai-langchain4j

一个基于 `Spring Boot 3` 与 `LangChain4j` 的 Java 智能体示例项目，面向“可对话、可调用工具、可接入知识库”的 AI 应用场景。当前项目已经串起了大模型对话、预约挂号工具、聊天记忆、基础 RAG 检索增强以及医疗场景安全拦截能力。

## 项目亮点

- 智能体对话：通过 `LangChain4j AiService` 组织系统提示词、记忆和工具调用
- 多模型接入：支持 `DashScope / OpenAI 兼容接口`，也支持本地 `Ollama`
- 工具调用：智能体可调用预约挂号相关工具完成业务操作
- 会话记忆：基于 `MongoDB` 保存聊天上下文
- 检索增强：提供基础 RAG 组件，为知识库问答预留能力
- 安全兜底：对医疗紧急场景做了优先拦截，避免直接进入普通对话流

## 适合做什么

这个项目适合作为下面几类场景的起点：

- Java 版 AI 助手 / 智能客服原型
- 带工具调用的业务型 Agent
- 接入知识库的问答系统
- 医疗、预约、导诊类实验项目

## 技术栈

- `Java 17`
- `Spring Boot 3.2.6`
- `LangChain4j 1.0.0-beta3`
- `MyBatis-Plus`
- `MongoDB`
- `MySQL`
- `Knife4j / OpenAPI`
- `Ollama`

## 核心能力

### 1. 对话入口

项目当前通过 `POST /xiaozhi/chat` 提供智能体对话入口，请求会进入 `XiaozhiController`，再交给 `XiaozhiAgent` 处理。

### 2. 智能体编排

`XiaozhiAgent` 通过 `LangChain4j` 显式绑定了这些能力：

- 流式聊天模型
- 聊天记忆提供器
- 预约挂号工具
- 检索增强组件

这意味着它不是一个只会“纯聊天”的模型封装，而是一个可以结合上下文、工具和知识的 Agent。

### 3. 预约挂号工具

项目中已经接入预约相关工具能力，支持的方向包括：

- 预约挂号
- 取消预约
- 查询是否有号源

这部分能力由 `AppointmentTools` 驱动，底层通过 `MySQL + MyBatis-Plus` 处理数据。

### 4. 会话记忆

聊天历史通过 `MongoDB` 持久化保存，适合做多轮对话、用户上下文延续和基础记忆管理。

### 5. 医疗安全拦截

`MedicalSafetyGuard` 会优先识别医疗紧急场景。如果用户输入命中高风险内容，系统会直接返回安全响应，而不是继续走普通对话逻辑。

## 项目结构

```text
src/main/java/com/example/javaailangchain4j
├─ assistant   智能体定义
├─ config      模型、记忆、向量检索等配置
├─ controller  HTTP 接口入口
├─ entity      业务实体
├─ mapper      数据访问层
├─ rag         检索增强相关组件
├─ safety      安全策略与拦截
├─ service     业务服务
├─ store       聊天记忆存储
└─ tool        智能体可调用工具
```

## 快速开始

### 环境要求

- `JDK 17`
- `Maven 3.9+`
- `MongoDB`
- `MySQL`
- 可选：`Ollama`

### 1. 配置模型与数据库

项目主配置文件位于 `src/main/resources/application.yaml`。

启动前至少需要检查这些配置：

- `spring.datasource.*`
- `spring.data.mongodb.uri`
- `langchain4j.*`

如果使用 DashScope 或兼容 OpenAI 的模型接口，先设置环境变量：

```powershell
$env:DASH_SCOPE_API_KEY="your_api_key"
```

### 2. 启动项目

```powershell
.\mvnw.cmd spring-boot:run
```

或：

```powershell
.\mvnw.cmd clean package
java -jar target\java-ai-langchain4j-0.0.1-SNAPSHOT.jar
```

默认端口：

```text
8080
```

### 3. 调用接口

```text
POST http://localhost:8080/xiaozhi/chat?id=1&message=你好
```

## 示例流程

一个典型请求链路大致如下：

1. 用户调用 `/xiaozhi/chat`
2. 控制器先进行医疗安全判断
3. 普通请求进入 `XiaozhiAgent`
4. Agent 结合提示词、记忆、检索和工具进行响应
5. 如有需要，调用预约挂号工具完成业务动作

## 当前已包含的模块

- `XiaozhiController`：对话入口
- `XiaozhiAgent`：智能体核心定义
- `AppointmentTools`：预约业务工具
- `MongoChatMemoryStore`：聊天记忆持久化
- `HybridSearchContentRetriever`：检索增强组件
- `MedicalSafetyGuard`：安全拦截

## 后续可以继续完善

- 增加更完整的 API 文档与调用示例
- 为 RAG 模块补充知识库导入说明
- 补齐自动化测试
- 将配置拆分为开发、测试、生产环境
- 增加前端演示页或聊天界面

## 提醒

当前仓库是公开仓库。若 `application.yaml` 中仍包含真实数据库账号、密码或其他敏感配置，建议尽快改为环境变量或本地覆盖配置，避免泄露。
