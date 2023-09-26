package com.github.open.discovery.kubernetes.client;

import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Collection;

/**
 * @author <a href="mailto:yanglu_u@126.com">dbses</a>
 * @since 1.0 2022/3/22
 */
public interface ClientManager {

    KubernetesClient getClient(String namespace);

    Collection<KubernetesClient> getClients();

}
