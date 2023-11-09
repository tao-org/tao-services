package ro.cs.tao.services;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import ro.cs.tao.persistence.AuditProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.services.interfaces.LogoutListener;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.user.LogEvent;
import ro.cs.tao.user.SessionDuration;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Set;
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
                final String userId = getUserId(hse);
                event.setUserId(userId);
                event.setTimestamp(LocalDateTime.now());
                event.setEvent("Logout");
                try {
                    auditProvider.save(event);
                    final Set<LogoutListener> listeners = ServiceRegistryManager.getInstance().getServiceRegistry(LogoutListener.class).getServices();
                    if (listeners != null) {
                        for (LogoutListener listener : listeners) {
                            final SessionDuration sessionDuration = auditProvider.getLastUserSession(userId);
                            final int processingTime = auditProvider.getLastSessionUserProcessingTime(userId);
                            listener.doAction(userId, sessionDuration, processingTime);
                        }
                    }
                } catch (PersistenceException e) {
                    logger.severe(e.getMessage());
                }
            }

            private String getUserId(HttpSessionEvent e) {
                final HttpSession session = e.getSession();
                SecurityContext context = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                return ((Principal) context.getAuthentication().getPrincipal()).getName();
            }
        };
    }
}
