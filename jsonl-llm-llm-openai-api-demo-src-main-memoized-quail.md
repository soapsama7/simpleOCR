# simpleOCR — 基于大模型的简易 OCR 工具开发计划

## Context

Audaque AI 辅助开发岗位考核。基于现有 Spring Boot 3.5.16 / Java 17 脚手架，开发一个调用 OpenAI 兼容大模型 API 的 OCR 工具。MVP 核心链路：用户截图 → 读取剪贴板图片 → 发送给后端 → 后端调用 LLM → 返回识别文本 → 自动写入剪贴板供粘贴。

---

## 技术决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| LLM 集成框架 | **Spring AI**（OpenAI starter） | 原生 Spring 生态，自动配置，支持多模态 |
| HTTP 客户端 | Spring AI 内置（底层 WebClient） | 无需额外依赖 |
| 配置格式 | **application.yml** | 用户要求，层次化配置更清晰 |
| 架构分层 | Controller → Service(Interface+Impl) → Spring AI | 经典三层 |
| 图片传输 | multipart/form-data（base64 备选） | REST 标准做法 |
| 剪贴板客户端 | 独立 Java main class（AWT + HttpClient） | 纯 Java 实现，跨平台 |
| 错误处理 | @RestControllerAdvice 全局异常处理 | Spring 最佳实践 |

---

## 项目最终结构（目标）

```
demo/
├── pom.xml                                      # 添加 spring-ai-openai-starter
├── src/main/resources/
│   └── application.yml                          # LLM 配置（apiKey/url/model）
└── src/main/java/com/audaque/demo/
    ├── DemoApplication.java                     # 启动类（已有）
    ├── config/
    │   └── OcrProperties.java                   # @ConfigurationProperties 读取配置
    ├── controller/
    │   └── OcrController.java                   # POST /api/ocr/recognize
    ├── service/
    │   ├── OcrService.java                      # 接口
    │   └── impl/
    │       └── OcrServiceImpl.java              # Spring AI ChatClient 调用
    ├── dto/
    │   └── OcrResponse.java                     # {success, text, model, elapsedMs}
    ├── exception/
    │   └── GlobalExceptionHandler.java          # 统一异常处理
    └── client/
        └── OcrClipboardClient.java              # 剪贴板交互工具（main 方法）
```

---

## 分轮实施计划

### Round 1 — 项目基础配置

**目标**：搭建 LLM 集成的基础设施

| 动作 | 文件 | 说明 |
|------|------|------|
| 改格式 | `application.properties` → `application.yml` | 已有 `spring.application.name=demo` 迁移 |
| 配置 LLM | `application.yml` | 添加 `spring.ai.openai` 段：api-key、base-url、chat.options.model |
| 加依赖 | `pom.xml` | 添加 `spring-ai-openai-spring-boot-starter`（版本需确认兼容 Spring Boot 3.5.16）+ Spring AI BOM |
| 配置类 | `OcrProperties.java` | `@ConfigurationProperties("ocr")` 读取自定义 OCR 参数（如默认提示词） |
| 验证 | 启动应用，确认 Spring AI 自动配置加载成功 |

**commit**: `[Round 1] 项目基础配置：application.yml + Spring AI OpenAI 依赖 + OcrProperties`

---

### Round 2 — OCR 核心服务层

**目标**：实现调用 LLM 进行图片文字识别的核心逻辑

| 动作 | 文件 | 说明 |
|------|------|------|
| DTO | `OcrResponse.java` | 字段：`success`, `text`, `model`, `elapsedMs` |
| 接口 | `OcrService.java` | 方法：`OcrResponse recognize(MultipartFile image)` |
| 实现 | `OcrServiceImpl.java` | 用 Spring AI 的 `ChatClient` + `UserMessage`（含图片 URL/base64）构造多模态请求，发送给 LLM，解析返回 |
| Model 配置 | 在 `OcrServiceImpl` 中注入 `OpenAiChatModel` 或使用 `ChatClient.Builder` |

**关键逻辑**：将 `MultipartFile` 转为 `org.springframework.ai.model.Media` 或 data URL，附加 OCR prompt，调用 `chatClient.call()`，提取文本。

**commit**: `[Round 2] OCR 核心服务层：OcrResponse DTO + OcrService 接口与实现`

---

### Round 3 — REST API 层 + 异常处理

**目标**：暴露 HTTP 端点，添加全局异常处理

| 动作 | 文件 | 说明 |
|------|------|------|
| Controller | `OcrController.java` | `POST /api/ocr/recognize`，接收 `@RequestParam("image") MultipartFile`，调用 OcrService，返回 OcrResponse |
| 异常处理 | `GlobalExceptionHandler.java` | 处理：文件为空、格式不支持、LLM 调用失败、通用异常 |
| 健康检查 | 同 Controller | 可选加 `GET /api/ocr/health` 检查模型连接状态 |

**commit**: `[Round 3] REST API 层：OcrController + GlobalExceptionHandler`

---

### Round 4 — 剪贴板交互客户端

**目标**：实现"截图 → 识别 → 剪贴板"的完整用户链路

| 动作 | 文件 | 说明 |
|------|------|------|
| 客户端 | `OcrClipboardClient.java` | `main()` 方法：`Toolkit.getDefaultToolkit().getSystemClipboard()` 读取图片 → `java.net.http.HttpClient` POST 到 `localhost:8080/api/ocr/recognize` → 将返回文本写回剪贴板 |

**流程**：
```
用户 Win+Shift+S 截图 → 图片在剪贴板 → 运行 OcrClipboardClient.main()
→ 调用本地 OCR API → 文本写回剪贴板 → Ctrl+V 粘贴
```

**commit**: `[Round 4] 剪贴板交互客户端：OcrClipboardClient`

---

### Round 5 — 测试与文档

**目标**：补充测试用例和 README

| 动作 | 说明 |
|------|------|
| 单元测试 | `OcrService` mock 测试 |
| 集成测试 | `OcrController` HTTP 测试 |
| README.md | 使用说明、配置方法、运行步骤 |

**commit**: `[Round 5] 测试与文档：单元测试 + README`

---

## 待确认/补充的点

1. **Spring AI 版本**：Spring Boot 3.5.16 对应的 Spring AI 版本需要在添加依赖时实测确认（预计 1.1.x 或更高），可能需要添加 Spring Milestone 仓库
2. **OCR Prompt 设计**：需要在 Service 中编写合适的 system/user prompt 引导 LLM 做 OCR（如"请识别图片中的文字，只输出识别结果，不要额外说明"）
3. **剪贴板客户端运行方式**：是打包成独立 jar 还是用 `mvn exec:java` 直接运行？建议直接运行，简化 MVP
4. **MySQL 依赖**：当前 pom.xml 有 mysql-connector-j，OCR 工具不需要——保留不动（避免无关变更），实际不使用
5. **图片大小限制**：是否需要在前端/后端做文件大小校验？建议 MVP 先不做，后续视情况加

---

## 验证方式

1. 配置有效的 OpenAI 兼容 API（或用 Ollama 本地模型 + OpenAI 兼容端点）
2. 启动 Spring Boot：`mvn spring-boot:run`
3. Win+Shift+S 截图
4. 运行 `OcrClipboardClient`
5. 在任意文本框 Ctrl+V，确认识别文本正确粘贴
