package com.github.open.discovery.autoconfig;

import com.github.open.discovery.kubernetes.KubernetesDiscoveryClient;
import com.github.open.discovery.kubernetes.reactive.KubernetesReactiveDiscoveryClient;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnReactiveDiscoveryEnabled;
import org.springframework.cloud.client.ReactiveCommonsClientAutoConfiguration;
import org.springframework.cloud.client.discovery.composite.reactive.ReactiveCompositeDiscoveryClientAutoConfiguration;
import org.springframework.cloud.client.discovery.simple.reactive.SimpleReactiveDiscoveryClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ReactiveDiscoveryClientAutoConfiguration
 *
 * @author Hailong Chang
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
@ConditionalOnReactiveDiscoveryEnabled
@AutoConfigureBefore({SimpleReactiveDiscoveryClientAutoConfiguration.class,
        ReactiveCommonsClientAutoConfiguration.class})
@AutoConfigureAfter({ReactiveCompositeDiscoveryClientAutoConfiguration.class,
        DiscoveryClientAutoConfiguration.class})
public class ReactiveDiscoveryClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KubernetesReactiveDiscoveryClient kubernetesReactiveDiscoveryClient(KubernetesDiscoveryClient kubernetesDiscoveryClient) {

        return new KubernetesReactiveDiscoveryClient(kubernetesDiscoveryClient);
    }


}
