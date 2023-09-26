package com.github.open.example.service;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="https://github.com/studeyang">studeyang</a>
 * @since 1.0 2022/11/7
 */
@Service
public class ExampleService implements InitializingBean {

    @Autowired
    private DiscoveryClient discoveryClient;

    @Override
    public void afterPropertiesSet() {
        System.out.println("===== Cloud Discovery Example =====");
        List<ServiceInstance> instance = discoveryClient.getInstances("courier-producer");
        System.out.println("instance: " + instance);
        System.out.println("ip: " + instance.stream().map(ServiceInstance::getHost).collect(Collectors.toList()));
    }
}
