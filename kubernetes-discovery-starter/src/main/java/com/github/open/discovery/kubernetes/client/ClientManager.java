package com.github.open.discovery.kubernetes.client;

import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Collection;

/**
 * @author <a href="https://github.com/studeyang">studeyang</a>
 * @since 1.0 2022/3/22
 */
public interface ClientManager {

    KubernetesClient getClient(String namespace);

    Collection<KubernetesClient> getClients();

}
