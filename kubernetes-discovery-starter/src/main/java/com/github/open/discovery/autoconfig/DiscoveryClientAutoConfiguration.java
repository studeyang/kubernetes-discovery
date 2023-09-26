package com.github.open.discovery.autoconfig;

import com.github.open.discovery.kubernetes.KubernetesDiscoveryClient;
import com.github.open.discovery.kubernetes.KubernetesDiscoveryProperties;
import com.github.open.discovery.kubernetes.client.ClientManager;
import com.github.open.discovery.kubernetes.client.KubernetesClientManager;
import com.github.open.discovery.kubernetes.client.KubernetesClientProperties;
import com.google.common.collect.Sets;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * @author <a href="https://github.com/studeyang">studeyang</a>
 * @since 1.0 2022/3/22
 */
@Configuration
@EnableConfigurationProperties({KubernetesClientProperties.class, KubernetesDiscoveryProperties.class})
public class DiscoveryClientAutoConfiguration {

    @Bean
    public ClientManager kubernetesClientManager(KubernetesClientProperties kubernetesClientProperties) {
        return new KubernetesClientManager(kubernetesClientProperties);
    }

    @Bean
    public KubernetesDiscoveryClient kubernetesDiscoveryClient(KubernetesDiscoveryProperties kubernetesDiscoveryProperties,
                                                               ClientManager clientManager,
                                                               KubernetesClientProperties kubernetesClientProperties,
                                                               ApplicationContext applicationContext) {
        Set<String> excludeServiceList = Sets.newHashSet();
        excludeServiceList.addAll(kubernetesClientProperties.getExcludeServices());

        return new KubernetesDiscoveryClient(clientManager, kubernetesDiscoveryProperties, excludeServiceList,
                applicationContext);
    }

}
