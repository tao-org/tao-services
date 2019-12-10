package ro.cs.tao.services.commons.update;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

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
        System.exit(0);
    }
}
