package com.github.open.discovery.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

/**
 * @author <a href="https://github.com/studeyang">studeyang</a>
 * @since 1.0 2022/3/22
 */
public class PropertyLoader {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyLoader.class);

    public static void loadYaml(ApplicationContext applicationContext, String propertySourceName, String fileName) {
        Environment environment = applicationContext.getEnvironment();
        if (environment instanceof ConfigurableEnvironment) {
            loadYaml((ConfigurableEnvironment) environment, propertySourceName, fileName);
        }
    }

    public static void loadYaml(ConfigurableEnvironment environment, String propertySourceName, String fileName) {
        try {
            YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
            List<PropertySource<?>> propertySourceList = loader.load(propertySourceName,
                    new ClassPathResource(fileName));
            for (PropertySource<?> propertySource : propertySourceList) {
                environment.getPropertySources().addLast(propertySource);
            }
        } catch (IOException e) {
            LOG.error("加载配置文件失败", e);
        }
    }
}
