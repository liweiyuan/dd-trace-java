package datadog.trace.instrumentation.scala;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activeScope;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activeSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.captureSpan;

import datadog.trace.api.Config;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.java.concurrent.State;
import datadog.trace.context.TraceScope;
import java.util.Collections;
import scala.util.Failure;
import scala.util.Success;
import scala.util.Try;

public class PromiseHelper {
  public static final boolean completionPriority =
      Config.get()
          .isIntegrationEnabled(
              Collections.singletonList("scala_promise_completion_priority"), false);

  public static AgentSpan getSpan() {
    AgentSpan span = null;
    final TraceScope scope = activeScope();
    if (null != scope && scope.isAsyncPropagating()) {
      if (scope instanceof AgentScope) {
        span = ((AgentScope) scope).span();
      } else {
        span = activeSpan();
      }
    }
    return span;
  }

  public static <T> Try<T> getTry(
      final Try<T> resolved, final AgentSpan span, final AgentSpan existing) {
    if (existing == span) {
      return resolved;
    }

    if (resolved instanceof Success) {
      Success<T> success = (Success<T>) resolved;
      return new Success<>(success.value());
    } else if (resolved instanceof Failure) {
      Failure<T> failure = (Failure<T>) resolved;
      return new Failure<>(failure.exception());
    }

    return resolved;
  }

  public static State handleSpan(final AgentSpan span, State state) {
    if (completionPriority && null != span) {
      TraceScope.Continuation continuation = captureSpan(span);
      TraceScope.Continuation existing = null;
      if (null != state) {
        existing = state.getAndResetContinuation();
      } else {
        state = State.FACTORY.create();
      }
      state.setOrCancelContinuation(continuation);
      if (null != existing) {
        existing.cancel();
      }
    }
    return state;
  }
}
