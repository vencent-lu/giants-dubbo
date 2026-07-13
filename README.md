# giants-dubbo

[![Maven Central](https://img.shields.io/maven-central/v/com.github.vencent-lu/giants-dubbo.svg?label=Maven%20Central)](https://search.maven.org/artifact/com.github.vencent-lu/giants-dubbo)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![JDK](https://img.shields.io/badge/JDK-1.7%2B-orange.svg)](https://www.oracle.com/java/)
[![Dubbo](https://img.shields.io/badge/Dubbo-2.6.12-brightgreen.svg)](https://dubbo.apache.org/)
[![Zipkin](https://img.shields.io/badge/Zipkin-v1%20API-informational.svg)](https://zipkin.io/)

giants-dubbo 是基于 [Apache Dubbo](https://dubbo.apache.org/)（`com.alibaba:dubbo:2.6.12`）的功能扩展组件，为 Dubbo 服务提供开箱即用的 **分布式链路追踪（Distributed Tracing）** 能力。

组件通过 Dubbo 的 SPI 扩展机制自动织入 Provider / Consumer 过滤器，并借助 [Zipkin](https://zipkin.io/) + [Brave](https://github.com/openzipkin/brave) 采集、上报调用链数据，无需修改任何业务代码即可完成接入。

## 特性

- **零侵入接入**：基于 Dubbo SPI（`META-INF/dubbo/com.alibaba.dubbo.rpc.Filter`）自动注册过滤器，只需在配置中启用。
- **全链路追踪**：覆盖 HTTP 入口 → Dubbo Consumer → Dubbo Provider 的完整调用链，跨进程透传 TraceId / SpanId。
- **异步上报**：使用 `AsyncReporter` + `OkHttpSender` 异步上报 span 到 Zipkin，对业务性能影响极小。
- **异常语义区分**：自动区分业务异常（`BusinessException`，标记为 `result`）与系统异常（标记为 `error`），便于在 Zipkin 中排查问题。
- **请求参数记录**：Consumer 端以 JSON 形式记录调用参数，便于追溯问题现场。

## 环境要求

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 1.7+ | 编译目标为 1.7 |
| Dubbo | 2.6.12 | `com.alibaba:dubbo` |
| Zipkin Server | 支持 v1 API（`/api/v1/spans`） | 用于收集与展示链路 |
| giants-common | 1.3.0 | 提供 `BusinessException` 等基础能力 |
| giants-web | 1.1.11（provided） | HTTP 入口过滤器 `AbstractFilter` 依赖 |

## 快速开始

### 1. 引入依赖

```xml
<dependency>
  <groupId>com.github.vencent-lu</groupId>
  <artifactId>giants-dubbo</artifactId>
  <version>1.0.2</version>
</dependency>
```

### 2. 配置 Zipkin 地址与应用名

在 Dubbo 配置文件（如 `dubbo.properties`）中设置：

```properties
# 当前应用在 Zipkin 中展示的服务名
dubbo.application.name=your-service-name
# Zipkin 收集端地址（v1 API）
dubbo.chain.trace.zipkin.address=http://127.0.0.1:9411/api/v1/spans
```

> 未配置时默认使用应用名 `default-dubbo-application` 与地址 `http://127.0.0.1:9411/api/v1/spans`。

### 3. 启用 Dubbo 过滤器

在 Provider 与 Consumer 上分别启用对应过滤器：

```xml
<!-- 服务提供方 -->
<dubbo:provider filter="providerTraceFilter" />

<!-- 服务消费方 -->
<dubbo:consumer filter="consumerTraceFilter" />
```

### 4.（可选）配置 HTTP 入口过滤器

若希望链路从 HTTP 请求入口发起，在 `web.xml` 中注册 `ChainTraceEntranceFilter`：

```xml
<filter>
  <filter-name>chainTraceEntranceFilter</filter-name>
  <filter-class>com.giants.dubbo.chain.trace.zipkin.filter.ChainTraceEntranceFilter</filter-class>
</filter>
<filter-mapping>
  <filter-name>chainTraceEntranceFilter</filter-name>
  <url-pattern>/*</url-pattern>
</filter-mapping>
```

完成后即可在 Zipkin UI 中查看完整调用链。

## 文档

- 📖 [框架使用手册](docs/USAGE.md) —— 架构说明、配置详解、集成步骤与常见问题

## 许可证

本项目基于 [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) 开源。
