# OTLP Tracing Guide

This document explains how distributed tracing is configured for the LinkGrove backend, how to run it locally, and which MDC fields enable correlation between logs, traces, and analytics workers.

## Overview

The backend uses **Micrometer Tracing** with the OpenTelemetry bridge to export spans to an OTLP collector (e.g., Jaeger). All configuration lives in Spring Boot `application.yml` and can be overridden via environment variables.

Key components:

- `TracingConfig` wires the OTLP exporter, sampler, and a trace-aware `Tracer` bean.
- `MdcLoggingFilter` propagates trace/span IDs into the SLF4J MDC so JSON logs contain `traceId`, `spanId`, and `service.instance.id`.
- `RequestIdFilter` adds `X-Request-Id` correlation across synchronous and asynchronous flows. The analytics worker carries this value when present.

## Configuration Knobs

| Property | Default | Description |
| --- | --- | --- |
| `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` | `1.0` | Probability (0.0–1.0) that requests generate spans. Lower to reduce volume in prod. |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4317` | gRPC endpoint for OTLP traces. Point to your collector (Jaeger, Tempo, etc.). |
| `OTEL_EXPORTER_OTLP_TIMEOUT` | `10s` | Timeout when sending spans. |
| `OTEL_SERVICE_NAME` | _auto-derived_ | Service name advertised to the collector. Defaults to `spring.application.name` (`linkgrove-api`). |
| `OTEL_RESOURCE_ATTRIBUTES` | _unset_ | Append custom resource attributes, e.g., `deployment.environment=prod`. |

Additional knobs are exposed in `TracingConfig` via `OTLPExporterProperties`—refer to the class for advanced options like gzip compression or batch size.

## Local Jaeger Setup

1. Navigate to `infra/docker`.
2. Start the observability stack: `./start.sh jaeger` (spins up Jaeger + dependencies).
3. Ensure the backend runs with the tracing profile (default settings already point to `localhost:4317`).
4. Visit Jaeger UI at `http://localhost:16686`. Search for service `linkgrove-api` to view traces.

### Manual Collector Startup

If you prefer using Docker directly:

```bash
docker run --rm -it \
  -e COLLECTOR_OTLP_ENABLED=true \
  -p 16686:16686 \
  -p 4317:4317 \
  jaegertracing/all-in-one:1.60
```

The backend will push spans to `http://localhost:4317` as long as the environment variable matches.

## MDC Correlation

`MdcLoggingFilter` sets the following MDC keys on every request:

- `traceId` / `spanId`: current OpenTelemetry identifiers.
- `service.instance.id`: distinguishes multiple instances in clustered deployments.
- `X-Request-Id`: user-supplied header or generated UUID.

Downstream components reuse these values:

- `WebhookService` forwards `X-Request-Id` when emitting webhooks.
- `AnalyticsWorker` attaches the request ID to its MDC context when processing queue messages.
- Integration tests ensure `RequestIdFilter` sets MDC for both web and worker threads.

### Asynchronous Workers

The AMQP listener (`AnalyticsWorker`) benefits from two features:

1. `TracingConfig` configures the RabbitMQ tracing instrumentation so publish/consume spans appear in the trace graph.
2. When a `LinkClickEvent` carries `requestId`, the worker places it in MDC, and logs for that processing span include the original request ID.

## Troubleshooting

- **No spans in Jaeger?** Verify the collector endpoint (`OTEL_EXPORTER_OTLP_ENDPOINT`) and confirm the container exposes port `4317`.
- **High cardinality warnings:** Reduce sampling probability or prune MDC fields in `TracingConfig` as needed.
- **Missing `traceId` in logs:** Ensure `MdcLoggingFilter` is registered early and that JSON log encoder is configured to include MDC fields (already covered in `logback-spring.xml`).

## Related Files

- `backend/src/main/java/com/linkgrove/api/config/TracingConfig.java`
- `backend/src/main/java/com/linkgrove/api/config/MdcLoggingFilter.java`
- `backend/src/main/java/com/linkgrove/api/config/RequestIdFilter.java`
- `infra/docker/docker-compose.yml` (Jaeger service)

