package ro.cs.tao.services.commons;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class WebConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(sitePrefix() + "/**").addResourceLocations(siteURIPath())
                .setCacheControl(CacheControl.maxAge(60, TimeUnit.MINUTES)
                                             .noTransform()
                                             .mustRevalidate());;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", sitePrefix() + "/" + defaultPage());
        WebMvcConfigurer.super.addViewControllers(registry);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        //Here we add our custom-configured HttpMessageConverter
        final int size = converters.size();
        for (int i = 0; i < size; i++) {
            if (converters.get(i) instanceof AbstractJacksonHttpMessageConverter) {
                converters.set(i, jacksonMessageConverter());
            }
        }
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorParameter(false);
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(-1);
        configurer.setTaskExecutor(new SimpleAsyncTaskExecutor());
    }

    protected abstract String siteURIPath();

    protected abstract String sitePrefix();

    protected abstract String defaultPage();

    protected JacksonJsonHttpMessageConverter jacksonMessageConverter(){
        JsonMapper mapper = JsonMapper.builder()
                .changeDefaultPropertyInclusion(incl ->
                        incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                .enable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
                .build();
        return new JacksonJsonHttpMessageConverter(mapper);
    }

}
