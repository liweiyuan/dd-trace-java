package datadog.trace.instrumentation.springwebflux.client;

import net.bytebuddy.asm.Advice;
import org.springframework.web.reactive.function.client.WebClient;

public class WebClientFilterAdvices {
  public static class AfterConstructorAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.This final WebClient.Builder thiz) {
      thiz.filters(WebClientTracingFilter::addFilter);
    }
  }

  public static class AfterFilterListModificationAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.This final WebClient.Builder thiz) {
      thiz.filters(WebClientTracingFilter::addFilter);
    }
  }
}
