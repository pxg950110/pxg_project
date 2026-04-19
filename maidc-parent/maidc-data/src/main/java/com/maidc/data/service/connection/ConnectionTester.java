package com.maidc.data.service.connection;

import java.util.Map;

public interface ConnectionTester {
    String getType();
    ConnectionTestResult test(Map<String, Object> params);
}
