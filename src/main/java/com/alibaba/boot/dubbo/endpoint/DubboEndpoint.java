package com.alibaba.boot.dubbo.endpoint;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.BeansException;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ReflectionUtils;

import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.config.spring.AnnotationBean;
import com.alibaba.dubbo.config.spring.ReferenceBean;

@ConfigurationProperties(prefix = "endpoints.dubbo", ignoreUnknownFields = false)
public class DubboEndpoint extends AbstractEndpoint<List<Object>> implements ApplicationContextAware {

    private List<ProviderBean> publishedInterfaceList  = new ArrayList<>();
    private List<ConsumerBean> subscribedInterfaceList = new ArrayList<>();

    private ApplicationContext context;

    public DubboEndpoint(){
        super("dubbo");
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    @Override
    public List<Object> invoke() {
        AnnotationBean annotationBean = context.getBean(AnnotationBean.class);
        Field serviceConfigsField = ReflectionUtils.findField(AnnotationBean.class, "serviceConfigs");
        Object services = ReflectionUtils.getField(serviceConfigsField, annotationBean);
        if (services instanceof List) {
            final Set<ServiceConfig<?>> serviceConfigs = (Set<ServiceConfig<?>>) services;
            for (ServiceConfig config : serviceConfigs) {
                ProviderBean providerBean = new ProviderBean();
                providerBean.setTarget(config.getStub());
                providerBean.setServiceInterface(config.getInterface());
                providerBean.setServiceVersion(config.getVersion());
                providerBean.setClientTimeout(config.getTimeout());
                providerBean.setMethodNames(config.getMethods());
                publishedInterfaceList.add(providerBean);
            }
        }
        Field referenceConfigsField = ReflectionUtils.findField(AnnotationBean.class, "referenceConfigs");
        Object references = ReflectionUtils.getField(referenceConfigsField, annotationBean);
        if (references instanceof ConcurrentMap) {
            final ConcurrentMap<String, ReferenceBean<?>> referenceConfigs = (ConcurrentMap<String, ReferenceBean<?>>) references;
            for (Entry<String, ReferenceBean<?>> reference : referenceConfigs.entrySet()) {
                ReferenceBean referenceBean = reference.getValue();
                ConsumerBean consumerBean = new ConsumerBean();
                consumerBean.setGroup(consumerBean.getGroup());
                consumerBean.setInterfaceName(consumerBean.getInterfaceName());
                consumerBean.setMethodNames(consumerBean.getMethodNames());
                consumerBean.setVersion(consumerBean.getVersion());
                subscribedInterfaceList.add(consumerBean);
            }
        }
        List<Object> all = new ArrayList<Object>();
        Map<String, List> provider = new HashMap<String, List>();
        provider.put("provider", publishedInterfaceList);
        Map<String, List> consumer = new HashMap<String, List>();
        consumer.put("consumer", subscribedInterfaceList);
        return all;
    }

}
