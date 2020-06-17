import datadog.trace.agent.test.AgentTestRunner
import datadog.trace.api.DDId
import datadog.trace.api.DDTags
import datadog.trace.api.interceptor.MutableSpan
import datadog.trace.api.sampling.PrioritySampling
import datadog.trace.context.TraceScope
import datadog.trace.core.propagation.ExtractedContext
import io.grpc.Context
import io.opentelemetry.OpenTelemetry
import io.opentelemetry.context.propagation.HttpTextFormat
import io.opentelemetry.trace.Status
import io.opentelemetry.trace.TracingContextUtils
import spock.lang.Subject

class OpenTelemetryTest extends AgentTestRunner {
  static {
    System.setProperty("dd.integration.opentelemetry-beta.enabled", "true")
  }

  @Subject
  def tracer = OpenTelemetry.tracerProvider.get("test-inst")
  def httpPropagator = OpenTelemetry.getPropagators().httpTextFormat

  def "test span"() {
    setup:
    def builder = tracer.spanBuilder("some name")
    if (tagBuilder) {
      builder.setAttribute(DDTags.RESOURCE_NAME, "some resource")
        .setAttribute("string", "a")
        .setAttribute("number", 1)
        .setAttribute("boolean", true)
    }
    if (addLink) {
      builder.setParent(tracer.converter.toSpanContext(new ExtractedContext(DDId.ONE, DDId.from(2), 0, null, [:], [:])))
    }
    def result = builder.startSpan()
    if (tagSpan) {
      result.setAttribute(DDTags.RESOURCE_NAME, "other resource")
      result.setAttribute("string", "b")
      result.setAttribute("number", 2)
      result.setAttribute("boolean", false)
    }
    if (exception) {
      result.setStatus(Status.UNKNOWN)
      result.setAttribute(DDTags.ERROR_MSG, (String) exception.message)
      result.setAttribute(DDTags.ERROR_TYPE, (String) exception.class.name)
      final StringWriter errorString = new StringWriter()
      exception.printStackTrace(new PrintWriter(errorString))
      result.setAttribute(DDTags.ERROR_STACK, errorString.toString())
    }

    expect:
    result instanceof MutableSpan
    (result as MutableSpan).localRootSpan == result.delegate
    (result as MutableSpan).isError() == (exception != null)
    tracer.currentSpan == null

    when:
    result.end()

    then:
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          if (addLink) {
            parentDDId(DDId.from(2))
          } else {
            parent()
          }
          serviceName "unnamed-java-app"
          operationName "test-inst"
          if (tagSpan) {
            resourceName "other resource"
          } else if (tagBuilder) {
            resourceName "some resource"
          } else {
            resourceName "some name"
          }
          errored exception != null
          tags {
            if (tagSpan) {
              "string" "b"
              "number" 2
              "boolean" false
            } else if (tagBuilder) {
              "string" "a"
              "number" 1
              "boolean" true
            }
            if (exception) {
              errorTags(exception.class)
            }
            defaultTags(addLink)
          }
          metrics {
            defaultMetrics()
          }
        }
      }
    }

    where:
    addLink | tagBuilder | tagSpan | exception
    false   | true       | false   | null
    true    | true       | true    | new Exception()
    false   | false      | false   | new Exception()
    true    | false      | true    | null
  }

  def "test scope"() {
    setup:
    def span = tracer.spanBuilder("some name").startSpan()
    def scope = tracer.withSpan(span)

    expect:
    scope instanceof TraceScope
    tracer.currentSpan.delegate == scope.delegate.span()

    when:
    scope.close()

    then:
    tracer.currentSpan == null

    cleanup:
    span.end()
  }

  def "test continuation"() {
    setup:
    def span = tracer.spanBuilder("some name").startSpan()
    TraceScope scope = tracer.withSpan(span)
    scope.setAsyncPropagation(true)

    expect:
    tracer.currentSpan.delegate == span.delegate

    when:
    def continuation = scope.capture()

    then:
    continuation instanceof TraceScope.Continuation

    when:
    scope.close()

    then:
    tracer.currentSpan == null

    when:
    scope = continuation.activate()

    then:
    tracer.currentSpan.delegate == span.delegate

    cleanup:
    scope.close()
    span.end()
  }

  def "test inject extract"() {
    setup:
    def span = tracer.spanBuilder("some name").startSpan()
    def context = TracingContextUtils.withSpan(span, Context.current())
    def textMap = [:]

    when:
    span.delegate.samplingPriority = contextPriority
    httpPropagator.inject(context, textMap, new TextMapSetter())

    then:
    textMap == [
      "x-datadog-trace-id"         : "$span.delegate.traceId",
      "x-datadog-parent-id"        : "$span.delegate.spanId",
      "x-datadog-sampling-priority": propagatedPriority.toString(),
    ]

    when:
    def extractedContext = httpPropagator.extract(context, textMap, new TextMapGetter())
    def extract = TracingContextUtils.getSpanWithoutDefault(extractedContext)

    then:
    extract.context.traceId == span.context.traceId
    extract.context.spanId == span.context.spanId
    extract.context.delegate.samplingPriority == propagatedPriority

    cleanup:
    span.end()

    where:
    contextPriority               | propagatedPriority
    PrioritySampling.SAMPLER_DROP | PrioritySampling.SAMPLER_DROP
    PrioritySampling.SAMPLER_KEEP | PrioritySampling.SAMPLER_KEEP
    PrioritySampling.UNSET        | PrioritySampling.SAMPLER_KEEP
    PrioritySampling.USER_KEEP    | PrioritySampling.USER_KEEP
    PrioritySampling.USER_DROP    | PrioritySampling.USER_DROP
  }

  static class TextMapGetter implements HttpTextFormat.Getter<Map<String, String>> {
    @Override
    String get(Map<String, String> carrier, String key) {
      return carrier.get(key)
    }
  }

  static class TextMapSetter implements HttpTextFormat.Setter<Map<String, String>> {
    @Override
    void set(Map<String, String> carrier, String key, String value) {
      carrier.put(key, value)
    }
  }
}
