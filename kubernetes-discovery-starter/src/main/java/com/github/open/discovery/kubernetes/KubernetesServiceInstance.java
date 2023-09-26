package com.github.open.discovery.kubernetes;

import com.google.common.collect.Maps;
import io.fabric8.kubernetes.api.model.Endpoints;
import lombok.Getter;
import lombok.ToString;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.style.ToStringCreator;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

/**
 * @author <a href="https://github.com/studeyang">studeyang</a>
 * @since 1.0 2022/3/22
 */
@ToString
public class KubernetesServiceInstance implements ServiceInstance {

    @Getter
    private final String instanceId;
    @Getter
    private final String serviceId;
    @Getter
    private final String host;
    @Getter
    private final int port;
    @Getter
    private final String namespace;

    private final Endpoints endpoints;

    public KubernetesServiceInstance(String serviceId, String host, int port, String namespace, Endpoints endpoints) {
        this.instanceId = host + "-" + port;
        this.serviceId = serviceId;
        this.host = host;
        this.port = port;
        this.endpoints = endpoints;
        this.namespace = namespace;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getScheme() {
        return "http";
    }

    @Override
    public URI getUri() {
        return URI.create(getUrl());
    }

    @Override
    public Map<String, String> getMetadata() {
        Map<String, String> metadata = Maps.newHashMap();
        metadata.putAll(getLabels());
        metadata.putAll(getAnnotations());
        return metadata;
    }

    public Map<String, String> getLabels() {
        return null != endpoints.getMetadata().getLabels()
                ? endpoints.getMetadata().getLabels()
                : Collections.emptyMap();
    }

    public Map<String, String> getAnnotations() {
        return null != endpoints.getMetadata().getAnnotations()
                ? endpoints.getMetadata().getAnnotations()
                : Collections.emptyMap();
    }

    private String getUrl() {
        return getScheme() + getHost() + ":" + getPort();
    }

}
