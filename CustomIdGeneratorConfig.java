import brave.Span;
import brave.Tracer;
import brave.internal.HexCodec;
import brave.propagation.TraceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class CustomIdGeneratorConfig {

    @Autowired
    Tracer tracer;

    @Bean
    public Tracer sleuthTracer() {
        return tracer;
    }

    @Bean
    public Random random() {
        return new Random();
    }

    @Bean
    public TraceContext.Extractor<Request> sleuthExtractor() {
        return (request, getter) -> {
            String traceId = request.getHeader("X-B3-TraceId");
            String spanId = request.getHeader("X-B3-SpanId");
            String parentSpanId = request.getHeader("X-B3-ParentSpanId");
            String sampled = request.getHeader("X-B3-Sampled");
            String flags = request.getHeader("X-B3-Flags");

            return TraceContext.newBuilder()
                    .traceId(traceId != null ? traceId : TraceContextOrSamplingFlags.nextTraceId(random()))
                    .spanId(spanId != null ? spanId : TraceContextOrSamplingFlags.nextSpanId(random()))
                    .parentId(parentSpanId)
                    .sampled(sampled != null ? sampled.equals("1") : TraceContextOrSamplingFlags.nextBoolean(random()))
                    .debug(flags != null && flags.equals("1"))
                    .build();
        };
    }

    @Bean
    public TraceContext.Injector<Response> sleuthInjector() {
        return (traceContext, response) -> {
            response.setHeader("X-B3-TraceId", traceContext.traceIdString());
            response.setHeader("X-B3-SpanId", traceContext.spanIdString());
            response.setHeader("X-B3-Sampled", traceContext.sampled() ? "1" : "0");
            response.setHeader("X-B3-Flags", traceContext.debug() ? "1" : "0");
        };
    }

    @Bean
    public CustomSpanCustomizer customSpanCustomizer() {
        return new CustomSpanCustomizer();
    }

    public static class CustomSpanCustomizer {

        public void customizeSpan(Span span) {
            // Customize span here if needed
        }
    }
}

class TraceContextOrSamplingFlags {
    static String nextTraceId(Random random) {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return HexCodec.toLowerHex(bytes);
    }

    static String nextSpanId(Random random) {
        long nextId = random.nextLong();
        return Long.toHexString(nextId);
    }

    static boolean nextBoolean(Random random) {
        return random.nextBoolean();
    }
}
