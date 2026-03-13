# IntentForge · 意铸

> **项目状态**  
> IntentForge 目前还是一个刚起步的工程项目，所有功能都还在持续开发中，首个 MVP 正在快速推进。如果你对这个项目感兴趣，欢迎持续关注后续更新。

[English](README.md) | [简体中文](README.zh-CN.md)

> **Think it. Forge it.**  
> **心有所想,即有所成**

**IntentForge** 是一个面向 AI 时代的、**意图驱动（Intent-Driven）** 的智能开发平台。  
它不是另一个聊天机器人，也不是某个单一 Agent 框架的包装层。  
它是一个围绕 **上下文工程（Context Engineering）、规范驱动（Spec-Driven）、云原生、Agent 治理、工具执行、上下文记忆、可观测、可审计** 构建的工程化平台内核。

你描述意图。  
IntentForge 负责把意图锻造成 **任务、计划、上下文、执行与决策、制品**，最终落成真实系统直接呈现。

---

## 什么是 IntentForge

大多数 AI 开发工具，解决的是 **“一个 agent 怎么更会做事”**。  
IntentForge 解决的是 **“多个 agent 怎么在统一治理下，把事稳定做成”**。

它负责：

- 将**意图**转化为任务、计划与工件
- 决定**谁执行**、**怎么执行**
- 提供**多视角**来观察执行过程
- 对过程和结果进行**验证、审计、回放与沉淀**
- 让配置、记忆、规则、工具与执行器实现**统一治理与可插拔**
- 统一纳管**原生 Agent** 与 **外部 Coding Agent**

IntentForge 不仅仅是与AI对话界面。    
而是一个 **AI Native Development Platform**。

---

## 模块说明

- governance：负责管
- agent：负责想
- channel：负责外部消息渠道接入、账号/会话路由与连接器可插拔
- prompt/model/model-provider：负责提示词资产、模型目录和提供方可插拔能力面
- tool：负责做
- memory/config：负责提供上下文
- session：负责会话生命周期、消息历史与归档检索
- space：负责公司/项目/产品/应用空间的隔离、多资源绑定与继承解析
- audit：负责留痕
- boot：负责启动
- desktop：负责桌面端适配

```text
intentforge/
├─ intentforge-bom                         # 统一依赖版本与构建基线
├─ intentforge-common                      # 通用基础能力：公共模型、枚举、常量、异常、工具类
├─ intentforge-api                         # 对外协议层：DTO、事件模型、AG-UI 协议对象
├─ intentforge-governance                  # 治理内核：编排、状态机、协调、调度、策略控制
├─ intentforge-audit                       # 审计与回放：run/step/tool-call/decision 全链路留痕
├─ intentforge-agent                       # Agent 聚合模块
│  ├─ intentforge-agent-core               # Agent 核心抽象：Task/Plan/ContextPack/Executor/Artifact/Decision
│  ├─ intentforge-agent-native             # 自研原生 coding agent 实现
│  ├─ intentforge-agent-springai           # 基于 Spring AI 的执行器实现
│  └─ intentforge-agent-external           # 外部 agent 适配器：opencode/codex/gemini/agentscope 等
├─ intentforge-channel                     # 渠道聚合模块
│  ├─ intentforge-channel-core             # 渠道描述、账号/目标/消息模型、路由契约、管理器/插件 SPI
│  ├─ intentforge-channel-local            # 内存渠道管理器 Provider、classpath SPI 加载、本地 plugins 目录加载
│  ├─ intentforge-channel-spring           # Spring factories 渠道插件发现桥接
│  └─ intentforge-channel-connectors       # 内置连接器实现与可复用连接器入口
├─ intentforge-prompt                      # 提示词聚合模块
│  ├─ intentforge-prompt-core              # 提示词定义、变量、查询条件、注册中心 SPI、管理器 Provider SPI
│  └─ intentforge-prompt-local             # 内存提示词管理 Provider、classpath SPI 加载、本地 plugins 目录加载
├─ intentforge-model                       # 模型聚合模块
│  ├─ intentforge-model-core               # 模型描述、能力标签、选择条件、注册中心 SPI、管理器 Provider SPI
│  └─ intentforge-model-local              # 内存模型管理 Provider、classpath SPI 加载、本地 plugins 目录加载
├─ intentforge-model-provider              # 模型提供方聚合模块
│  ├─ intentforge-model-provider-core      # Provider 描述、Provider SPI、Provider 注册中心约定、注册中心 Provider SPI
│  └─ intentforge-model-provider-local     # 内存 Provider 注册中心 Provider、本地插件加载、元数据/版本校验
├─ intentforge-tool                        # 工具聚合模块
│  ├─ intentforge-tool-core                # Tool SPI、ToolGateway、权限模型、工具注册中心
│  ├─ intentforge-tool-mcp                 # MCP 协议接入与 MCP 工具桥接
│  ├─ intentforge-tool-connectors          # 外部连接器：Git/Jira/Repo/File/DB 等
│  ├─ intentforge-tool-fs                  # 文件系统工具：读写、diff、patch、快照、worktree/sandbox
│  ├─ intentforge-tool-shell               # 命令执行工具：进程管理、stdout/stderr、超时、中断
│  └─ intentforge-tool-validation          # 验证工具：构建、测试、lint、规则校验、结果验收
├─ intentforge-memory                      # 记忆聚合模块
│  ├─ intentforge-memory-core              # 记忆抽象：MemoryStore、MemoryStrategy、MemoryOrchestrator
│  ├─ intentforge-memory-file              # 文件记忆实现：JSON/YAML/Markdown/NDJSON
│  ├─ intentforge-memory-sql               # SQL 记忆实现：SQLite/PostgreSQL
│  └─ intentforge-memory-graph             # 图谱记忆实现：实体关系、知识依赖、图检索
├─ intentforge-config                      # 配置聚合模块
│  ├─ intentforge-config-core              # 配置抽象：ConfigProvider、ConfigResolver、Overlay
│  ├─ intentforge-config-file              # 文件配置实现：YAML/JSON 本地配置
│  ├─ intentforge-config-db                # 数据库配置实现：运行时配置、策略配置、分层配置
│  └─ intentforge-config-graph             # 图配置实现：复杂依赖与关系型规则配置
├─ intentforge-session                     # 会话聚合模块
│  ├─ intentforge-session-core             # 会话快照、消息模型、查询契约、未找到异常与管理器 Provider SPI
│  └─ intentforge-session-local            # 内存会话管理器、classpath Provider 选择与本地运行时装配
├─ intentforge-space                       # 空间聚合模块
│  ├─ intentforge-space-core               # 空间层级模型、资源绑定 Profile、SPI 契约、异常与解析结果定义
│  └─ intentforge-space-local              # 内存空间注册表、多资源继承解析器与本地运行时装配
├─ intentforge-boot                        # 启动聚合模块
│  ├─ intentforge-boot-local               # 本地模式启动入口：桌面端本地内核与 space/session 感知启动
│  └─ intentforge-boot-server              # 服务端模式启动入口：远程服务与平台部署
├─ intentforge-desktop                     # 桌面适配聚合模块
│  ├─ intentforge-desktop-core             # 桌面适配公共抽象：IDE/终端/目录/文件联动能力
│  ├─ intentforge-desktop-windows          # Windows 桌面集成适配
│  ├─ intentforge-desktop-macos            # macOS 桌面集成适配
│  └─ intentforge-desktop-linux            # Linux 桌面集成适配
├─ frontend                                # 前端工程
│  ├─ desktop                              # 桌面壳前端：Electron/Tauri 容器层
│  └─ ui                                   # 通用 UI 前端：对话、面板、工件、状态展示
├─ python                                  # Python 运行时扩展
│  └─ runtime                              # Python runtime：承载特定 AI/数据/执行场景
└─ scripts                                 # 构建、启动、发布、开发辅助脚本
```
