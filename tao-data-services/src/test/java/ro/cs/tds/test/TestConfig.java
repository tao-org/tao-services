package ro.cs.tds.test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
//        excludeFilters = {
//                // Exclude the default message service
//                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = DefaultMessageService.class),
//                // Exclude the default boot application or it's
//                // @ComponentScan will pull in the default message service
//                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = IntegrationTestDemo.class)
//        }
)
public class TestConfig {

//    @Bean
//        // Define our own test message service
//    MessageService mockMessageService() {
//        return new MessageService() {
//            @Override
//            public String getMessage() {
//                return "This is a test message";
//            }
//        };
//    }
}