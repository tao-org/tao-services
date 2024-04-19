package ro.cs.tao.services.commons.update;

import org.springframework.beans.BeansException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import ro.cs.tao.configuration.ConfigurationManager;

import java.util.logging.Logger;

@Service("restartService")
public class RestartService implements ApplicationContextAware {
    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    public void doRestart() {
        Logger.getLogger(RestartController.class.getName()).info("Services restart initiated");
        ((ConfigurableApplicationContext) context).close();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        RestTemplateBuilder builder = new RestTemplateBuilder();
        String response = builder.build()
                                 .postForObject(ConfigurationManager.getInstance().getValue("tao.services.base") + "/actuator/restart",
                                                entity, String.class);
        Logger.getLogger(RestartController.class.getName()).info("Services are restarting");
    }
}
