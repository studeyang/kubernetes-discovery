package com.github.open.discovery.kubernetes.client;

import com.google.common.collect.Lists;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author jian.xu
 * @author <a href="https://github.com/studeyang">studeyang</a>
 */
@Data
@ConfigurationProperties("icloud.kubernetes")
public class KubernetesClientProperties {

    private String apiServerUrl = "https://kubernetes.default";
    private boolean insecureSkipTlsVerify;
    private String caCertFileUrl;
    private String oauth2Token;
    private List<String> includeNamespaces = Lists.newArrayList();
    /**
     * 手动排除的微服务名<br/>
     * 一般不配置，组件去维护这个列表成本太大，可通过 discovery.disabled: true 禁用服务发现
     */
    private List<String> excludeServices = Lists.newArrayList();

}
