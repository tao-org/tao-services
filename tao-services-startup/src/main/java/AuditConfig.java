import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import ro.cs.tao.persistence.AuditProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.user.LogEvent;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.ws.rs.core.SecurityContext;
import java.time.LocalDateTime;
import java.util.logging.Logger;

@Configuration
public class AuditConfig {

    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    private AuditProvider auditProvider;

    @Bean
    public HttpSessionListener httpSessionListener() {
        return new HttpSessionListener() {
            final Logger logger = Logger.getLogger(HttpSessionListener.class.getName());
            @Override
            public void sessionCreated(HttpSessionEvent hse) {
                // NO-OP, login is handled in the AuthenticationController
            }

            @Override
            public void sessionDestroyed(HttpSessionEvent hse) {
                final LogEvent event = new LogEvent();
                event.setUserId(getUserId(hse));
                event.setTimestamp(LocalDateTime.now());
                event.setEvent("Logout");
                try {
                    auditProvider.save(event);
                } catch (PersistenceException e) {
                    logger.severe(e.getMessage());
                }
            }

            private String getUserId(HttpSessionEvent e) {
                final HttpSession session = e.getSession();
                SecurityContext context = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                return context.getUserPrincipal().getName();
            }
        };
    }
}
