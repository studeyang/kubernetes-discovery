package com.github.open.discovery.kubernetes.client;

import com.google.common.collect.Maps;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;

/**
 * @author <a href="https://github.com/studeyang">studeyang</a>
 * @since 1.0 2022/3/22
 * <p>
 * KubernetesClient 实例管理器，每个 namespace 对应一个 KubernetesClient
 */
@Slf4j
public class KubernetesClientManager implements ClientManager {

    /**
     * <p>key - namespace, 这里要求所有群集的 namespace 都不能重复</p>
     * <p>这里保证配置在前面的 namespace 优先加载</p>
     */
    @Getter
    protected Map<String, KubernetesClient> clientMap = Maps.newLinkedHashMap();

    public KubernetesClientManager(KubernetesClientProperties kubernetesClientProperties) {

        // 遍历 namespace
        for (String namespace : kubernetesClientProperties.getIncludeNamespaces()) {
            if (clientMap.containsKey(namespace)) {
                log.error("namespace [{}] 已存在", namespace);
                continue;
            }
            clientMap.put(namespace, createKubernetesClient(kubernetesClientProperties, namespace));
        }
    }

    @Override
    public KubernetesClient getClient(String namespace) {
        return clientMap.get(namespace);
    }

    @Override
    public Collection<KubernetesClient> getClients() {
        return clientMap.values();
    }

    protected KubernetesClient createKubernetesClient(KubernetesClientProperties properties, String namespace) {
        ConfigBuilder builder = new ConfigBuilder();

        if (StringUtils.isNotBlank(properties.getApiServerUrl())) {
            builder = builder.withMasterUrl(properties.getApiServerUrl());
        }

        if (StringUtils.isNotBlank(properties.getCaCertFileUrl())) {
            builder = builder.withCaCertFile(properties.getCaCertFileUrl());
        }

        if (StringUtils.isNotBlank(properties.getOauth2Token())) {
            builder = builder.withOauthToken(properties.getOauth2Token());
        }

        if (StringUtils.isNotBlank(namespace)) {
            builder = builder.withNamespace(namespace);
        }

        if (properties.isInsecureSkipTlsVerify()) {
            builder = builder.withTrustCerts(true);
        }

        return new DefaultKubernetesClient(builder.build());
    }


}
