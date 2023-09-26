package com.github.open.discovery.kubernetes.reactive;

import com.github.open.discovery.kubernetes.KubernetesDiscoveryClient;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * Kubernetes Reactive Discovery Client implementation
 *
 * @author Hailong Chang
 * @version 1.0
 */
public class KubernetesReactiveDiscoveryClient implements ReactiveDiscoveryClient {


    private KubernetesDiscoveryClient kubernetesDiscoveryClient;

    public KubernetesReactiveDiscoveryClient(KubernetesDiscoveryClient kubernetesDiscoveryClient) {
        this.kubernetesDiscoveryClient = kubernetesDiscoveryClient;
    }

    @Override
    public String description() {
        return "Kubernetes Reactive Discovery Client";
    }

    @Override
    public Flux<ServiceInstance> getInstances(String serviceId) {
        Assert.notNull(serviceId, "[Assertion failed] - the object argument must not be null");
        return Flux.defer(() -> Flux.fromIterable(kubernetesDiscoveryClient.getInstances(serviceId)))
                .subscribeOn(Schedulers.boundedElastic());
    }


    @Override
    public Flux<String> getServices() {
        return Flux.defer(() -> Flux.fromIterable(kubernetesDiscoveryClient.getServices()))
                .subscribeOn(Schedulers.boundedElastic());
    }

}
