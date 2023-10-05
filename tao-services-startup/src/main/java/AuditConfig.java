import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ro.cs.tao.utils.Tuple;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class AuditConfig {
    private static final Map<String, Tuple<LocalDateTime, LocalDateTime>> sessions = new HashMap<>();

    @Autowired
    private MeterRegistry meterRegistry;

    @Bean
    public HttpSessionListener httpSessionListener() {
        return new HttpSessionListener() {
            @Override
            public void sessionCreated(HttpSessionEvent hse) {
                sessions.put(hse.getSession().getId(), new Tuple<>(LocalDateTime.now(), null));
            }

            @Override
            public void sessionDestroyed(HttpSessionEvent hse) {
                sessions.remove(hse.getSession().getId());
            }
        };
    }
}
