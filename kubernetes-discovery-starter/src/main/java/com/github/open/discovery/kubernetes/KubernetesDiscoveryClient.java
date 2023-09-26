package com.github.open.discovery.kubernetes;

import com.github.open.discovery.kubernetes.client.ClientManager;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author jian.xu
 * @author <a href="https://github.com/studeyang">studeyang</a>
 */
@Slf4j
public class KubernetesDiscoveryClient implements DiscoveryClient {

    private final ClientManager clientManager;
    private final KubernetesDiscoveryProperties properties;
    private final Set<String> excludeServices;
    /**
     * 服务名列表
     */
    private Map<String, KubernetesService> serviceMap;
    /**
     * 服务实例列表
     */
    private LoadingCache<String, List<ServiceInstance>> instanceCache;

    private ApplicationContext applicationContext;

    public KubernetesDiscoveryClient(ClientManager clientManager,
                                     KubernetesDiscoveryProperties properties,
                                     Set<String> excludeServices,
                                     ApplicationContext applicationContext) {
        this.clientManager = clientManager;
        this.properties = properties;
        this.excludeServices = excludeServices;
        this.applicationContext = applicationContext;

        serviceMap = getServiceMap();

        ScheduledExecutorService fetchServiceScheduler = new ScheduledThreadPoolExecutor(1,
                new ThreadFactoryBuilder()
                        .setNameFormat("Kubernetes-ServiceRefreshExecutor-%d")
                        .setDaemon(true)
                        .build());
        fetchServiceScheduler.scheduleAtFixedRate(
                () -> serviceMap = getServiceMap(),
                properties.getFetchServiceIntervalSeconds(),
                properties.getFetchServiceIntervalSeconds(),
                TimeUnit.SECONDS);

        this.instanceCache = CacheBuilder.newBuilder()
                // 缓存失效时间
                .refreshAfterWrite(properties.getFetchInstanceIntervalSeconds(), TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<ServiceInstance>>() {
                    @Override
                    public List<ServiceInstance> load(@NotNull String serviceId) {
                        return getInstances(serviceMap.get(serviceId));
                    }
                });

    }

    @Override
    public String description() {
        return "Kubernetes Discovery Client";
    }

    @Override
    public List<String> getServices() {
        return Lists.newArrayList(serviceMap.keySet());
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceId) {
        if (StringUtils.isBlank(serviceId)) {
            log.warn("no serviceId");
            return Collections.emptyList();
        }

        if (!serviceMap.containsKey(serviceId)) {
            log.warn("no KubernetesService, serviceId: '{}'", serviceId);
            return Collections.emptyList();
        }

        try {
            return instanceCache.get(serviceId);
        } catch (ExecutionException e) {
            log.warn("从缓存中获取", e);
            return Collections.emptyList();
        }
    }

    private Map<String, KubernetesService> getServiceMap() {

        log.debug("------ Fetching Services From Kubernetes");

        // serviceMap 指的就是 Kubernetes 的 service
        Map<String, KubernetesService> kubernetesServiceMap = Maps.newHashMap();

        List<KubernetesService> addFailServiceList = new ArrayList<>();
        clientManager.getClients()
                .stream()
                .flatMap(client -> client.services().list().getItems().stream()
                        .map(KubernetesService::new)
                        .filter(KubernetesService::isEffective)
                )
                .collect(Collectors.toList())
                .forEach(kubernetesService -> {

                    if (kubernetesServiceMap.containsKey(kubernetesService.getId())) {
                        addFailServiceList.add(kubernetesService);
                    }

                    // 如果该 serviceId 没被排除
                    else if (!excludeServices.contains(kubernetesService.getId())) {
                        kubernetesServiceMap.put(kubernetesService.getId(), kubernetesService);
                    }
                });

        List<String> logs = Lists.newArrayList();
        for (KubernetesService kubernetesService : addFailServiceList) {
            String serviceIdAddFailLog = String.format("Services: [%s](%s) 添加失败，存在同名 serviceId: [%s](%s)",
                    kubernetesService.getNamespace(),
                    kubernetesService.getName(),
                    kubernetesServiceMap.get(kubernetesService.getId()).getNamespace(),
                    kubernetesService.getId());
            log.debug(serviceIdAddFailLog);
            logs.add(serviceIdAddFailLog);
        }
        log.debug("{}", logs);
        log.debug("service map: {}", kubernetesServiceMap);

        if (this.serviceMap == null || this.serviceMap.size() != kubernetesServiceMap.size()) {
            applicationContext.publishEvent(new ServicesUpdateEvent(kubernetesServiceMap.keySet()));
        }

        return kubernetesServiceMap;
    }

    private List<ServiceInstance> getInstances(KubernetesService kubernetesService) {

        String serviceId = kubernetesService.getId();
        log.debug("------ Fetching Instances From Kubernetes, serviceId: '{}'", serviceId);

        List<ServiceInstance> instances = Lists.newArrayList();

        if (!kubernetesService.isEffective()) {
            log.warn("kubernetesService is invalid, serviceId: {}", serviceId);
            this.serviceMap.remove(serviceId);
            return instances;
        }

        String namespace = kubernetesService.getNamespace();
        KubernetesClient client = clientManager.getClient(namespace);
        if (null == client) {
            log.warn("--- no KubernetesClient, namespace: '{}'", namespace);
            return instances;
        }

        Endpoints endpoints = client.endpoints()
                .inNamespace(namespace)
                .withName(kubernetesService.getName())
                .get();

        if (null == endpoints) {
            log.warn("No Endpoints Found, serviceId: {}", serviceId);
            return instances;
        }

        List<EndpointSubset> subsets = getSubsetsFromEndpoints(endpoints);

        if (!subsets.isEmpty()) {
            for (EndpointSubset subset : subsets) {
                List<EndpointAddress> addresses = subset.getAddresses();
                EndpointPort endpointPort = findEndpointPort(subset);
                for (EndpointAddress endpointAddress : addresses) {
                    instances.add(
                            new KubernetesServiceInstance(
                                    serviceId,
                                    endpointAddress.getIp(),
                                    endpointPort.getPort(),
                                    namespace,
                                    endpoints));
                }
            }
        }

        return instances;
    }

    private List<EndpointSubset> getSubsetsFromEndpoints(Endpoints endpoints) {
        if (endpoints == null) {
            return new ArrayList<>();
        }
        if (endpoints.getSubsets() == null) {
            return new ArrayList<>();
        }

        return endpoints.getSubsets();
    }

    private EndpointPort findEndpointPort(EndpointSubset s) {
        List<EndpointPort> ports = s.getPorts();
        EndpointPort endpointPort;
        if (ports.size() == 1) {
            endpointPort = ports.get(0);
        } else {
            Predicate<EndpointPort> portPredicate;
            if (!StringUtils.isEmpty(properties.getPrimaryPortName())) {
                portPredicate = port -> properties.getPrimaryPortName()
                        .equalsIgnoreCase(port.getName());
            } else {
                portPredicate = port -> true;
            }
            endpointPort = ports.stream().filter(portPredicate).findAny()
                    .orElseThrow(IllegalStateException::new);
        }
        return endpointPort;
    }

}
