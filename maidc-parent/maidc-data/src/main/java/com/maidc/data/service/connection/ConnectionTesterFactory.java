package com.maidc.data.service.connection;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ConnectionTesterFactory {

    private final Map<String, ConnectionTester> testerMap;

    public ConnectionTesterFactory(List<ConnectionTester> testers) {
        this.testerMap = testers.stream()
                .collect(Collectors.toMap(ConnectionTester::getType, Function.identity()));
    }

    public ConnectionTester getTester(String testCommand) {
        return testerMap.get(testCommand);
    }
}
