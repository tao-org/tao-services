/*
 * Copyright (C) 2018 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package ro.cs.tao.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;
import ro.cs.tao.configuration.ConfigurationManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@EnableWebMvc
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String siteBase = ConfigurationManager.getInstance().getValue("site.location", "static");
        Path sitePath = Paths.get(siteBase);
        if (!sitePath.isAbsolute()) {
            sitePath = TaoServicesStartup.homeDirectory().resolve(siteBase).toAbsolutePath();
        }
        registry.addResourceHandler("/ui/**").addResourceLocations(sitePath.toUri().toString());
        ConfigurationManager.getInstance().setValue("site.path", sitePath.toString());
        ConfigurationManager.getInstance().setValue("site.url", sitePath.toUri().toString());
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/ui/login.html");
        WebMvcConfigurer.super.addViewControllers(registry);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        //Here we add our custom-configured HttpMessageConverter
        for (int i = 0; i < converters.size(); i++) {
            if (converters.get(i) instanceof MappingJackson2HttpMessageConverter) {
                converters.set(i, jacksonMessageConverter());
            }
        }
        //converters.add(jacksonMessageConverter());
    }

    /*@Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        //Here we add our custom-configured HttpMessageConverter
        converters.add(jacksonMessageConverter());
        converters.add(new ResourceHttpMessageConverter());
        converters.add(new MappingJackson2XmlHttpMessageConverter());
    }*/

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(false);
    }

    private MappingJackson2HttpMessageConverter jacksonMessageConverter(){
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        ObjectMapper mapper = new ObjectMapper();
        //Registering Hibernate5Module to support lazy objects
        Hibernate5Module hibernate5Module = new Hibernate5Module();
        hibernate5Module.disable(Hibernate5Module.Feature.USE_TRANSIENT_ANNOTATION);
        hibernate5Module.enable(Hibernate5Module.Feature.FORCE_LAZY_LOADING);
        mapper.registerModule(hibernate5Module);
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        messageConverter.setObjectMapper(mapper);
        return messageConverter;
    }
}
