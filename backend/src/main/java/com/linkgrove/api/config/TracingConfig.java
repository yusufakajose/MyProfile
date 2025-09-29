package com.linkgrove.api.config;

import io.micrometer.tracing.BaggageManager;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.propagation.Propagator;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import io.opentelemetry.semconv.ServiceAttributes;

@Configuration
@ConditionalOnProperty(prefix = "management.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TracingConfig {

    private static final String INSTRUMENTATION_NAME = "com.linkgrove.api";

    @Value("${spring.application.name:linkgrove-api}")
    private String applicationName;

    @Value("${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4317}")
    private String otlpEndpoint;

    @Value("${management.tracing.sampling.probability:1.0}")
    private double samplingProbability;

    @Bean(destroyMethod = "shutdown")
    public SpanExporter otlpSpanExporter() {
        return OtlpGrpcSpanExporter.builder()
                .setEndpoint(otlpEndpoint)
                .setTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    public Resource otelResource() {
        Attributes attributes = Attributes.builder()
                .put(ServiceAttributes.SERVICE_NAME, applicationName)
                .put(AttributeKey.stringKey("service.instance.id"), UUID.randomUUID().toString())
                .build();
        return Resource.getDefault().merge(Resource.create(attributes));
    }

    @Bean
    public Sampler otelSampler() {
        if (samplingProbability >= 1.0d) {
            return Sampler.alwaysOn();
        }
        if (samplingProbability <= 0.0d) {
            return Sampler.alwaysOff();
        }
        return Sampler.traceIdRatioBased(samplingProbability);
    }

    @Bean(destroyMethod = "close")
    public SdkTracerProvider sdkTracerProvider(Resource otelResource, SpanExporter otlpSpanExporter, Sampler otelSampler) {
        return SdkTracerProvider.builder()
                .setResource(otelResource)
                .setSampler(otelSampler)
                .addSpanProcessor(BatchSpanProcessor.builder(otlpSpanExporter).build())
                .build();
    }

    @Bean
    public ContextPropagators contextPropagators() {
        TextMapPropagator propagator = TextMapPropagator.composite(
                W3CTraceContextPropagator.getInstance(),
                W3CBaggagePropagator.getInstance()
        );
        return ContextPropagators.create(propagator);
    }

    @Bean
    public OpenTelemetry openTelemetry(SdkTracerProvider sdkTracerProvider, ContextPropagators contextPropagators) {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(contextPropagators)
                .buildAndRegisterGlobal();
    }

    @Bean
    public io.opentelemetry.api.trace.Tracer otelTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(INSTRUMENTATION_NAME);
    }

    @Bean
    public OtelCurrentTraceContext otelCurrentTraceContext() {
        return new OtelCurrentTraceContext();
    }

    @Bean
    public BaggageManager baggageManager(OtelCurrentTraceContext otelCurrentTraceContext) {
        return new OtelBaggageManager(otelCurrentTraceContext, List.of(), List.of());
    }

    @Bean
    public Tracer micrometerTracer(io.opentelemetry.api.trace.Tracer otelTracer,
                                   OtelCurrentTraceContext otelCurrentTraceContext,
                                   BaggageManager baggageManager) {
        return new OtelTracer(otelTracer, otelCurrentTraceContext, event -> { }, baggageManager);
    }

    @Bean
    @Primary
    public Propagator micrometerPropagator(ContextPropagators contextPropagators,
                                           io.opentelemetry.api.trace.Tracer otelTracer) {
        return new OtelPropagator(contextPropagators, otelTracer);
    }
}


