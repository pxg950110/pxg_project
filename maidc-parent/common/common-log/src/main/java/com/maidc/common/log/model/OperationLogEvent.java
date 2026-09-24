package com.maidc.common.log.model;

import org.springframework.context.ApplicationEvent;

public class OperationLogEvent extends ApplicationEvent {

    private final OperationLogData logData;

    public OperationLogEvent(Object source, OperationLogData logData) {
        super(source);
        this.logData = logData;
    }

    public OperationLogData getLogData() {
        return logData;
    }
}
