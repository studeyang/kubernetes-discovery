package com.github.open.discovery.kubernetes;

import org.springframework.context.ApplicationEvent;

import java.util.Set;

public class ServicesUpdateEvent extends ApplicationEvent {
    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public ServicesUpdateEvent(Set<String> source) {
        super(source);
    }

    @Override
    public Set<String> getSource() {
        return (Set<String>) super.getSource();
    }
}
