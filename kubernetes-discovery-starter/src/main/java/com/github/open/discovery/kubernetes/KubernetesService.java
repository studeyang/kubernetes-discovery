package com.github.open.discovery.kubernetes;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.style.ToStringCreator;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author jian.xu
 */
@Slf4j
public class KubernetesService {

    private static final String LABEL_DISABLED = "discovery.disabled";
    private static final String SERVICE_ID_KEY = "serviceId";

    @Getter
    private String id;
    @Getter
    private String name;
    @Getter
    private String namespace;
    private final boolean effective;

    public KubernetesService(Service service) {

        this.effective = checkKubernetesService(service);

        if (this.effective) {
            ObjectMeta metadata = service.getMetadata();
            Map<String, String> labels = service.getMetadata().getLabels();
            labels.forEach((k, v) -> {
                if (k.contains(SERVICE_ID_KEY) && StringUtils.isNotEmpty(labels.get(k))) {
                    this.id = labels.get(k);
                }
            });
            this.name = metadata.getName();
            this.namespace = metadata.getNamespace();
        }
    }

    public KubernetesService self() {
        return this;
    }

    public boolean isEffective() {
        return this.effective;
    }

    /**
     * 检查必要的标签信息
     *
     * @param service 服务
     * @return 检查结果
     */
    private boolean checkKubernetesService(Service service) {
        try {
            ObjectMeta metadata = service.getMetadata();

            if (null == metadata.getName()) {
                log.warn("no named service");
                return false;
            }

            String serviceName = metadata.getName();
            Map<String, String> labels = metadata.getLabels();

            if (null == labels || labels.isEmpty()) {
                log.warn("service is invalid, no labels: '{}'", serviceName);
                return false;
            }

            /*
             * 1、显示申明标签 discovery.disabled=true 时，服务不会被所有网关发现（全局禁用）
             * 2、显示申明标签 discovery.disabled.gateway=true 时，服务不会被内网网关发现
             */
            if (labels.containsKey(LABEL_DISABLED)
                    && "true".equalsIgnoreCase(labels.get(LABEL_DISABLED))) {
                log.debug("service is disabled: '{}'", serviceName);
                return false;
            }

            AtomicReference<String> serviceIdKey = new AtomicReference<>("");
            labels.forEach((key, value) -> {
                if (key.contains(SERVICE_ID_KEY)) {
                    serviceIdKey.set(key);
                }
            });
            if (serviceIdKey.get().isEmpty()) {
                log.warn("no serviceId label, service is invalid: '{}'", serviceName);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("服务有效性判定执行错误！", e);
            return false;
        }

    }

    @Override
    public String toString() {
        return new ToStringCreator(this).toString();
    }

}
