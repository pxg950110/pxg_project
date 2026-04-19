package com.maidc.data.service;

import com.maidc.data.entity.DataSourceEntity;
import com.maidc.data.entity.DataSourceTypeEntity;
import com.maidc.data.repository.DataSourceRepository;
import com.maidc.data.service.connection.ConnectionTestResult;
import com.maidc.data.service.connection.ConnectionTester;
import com.maidc.data.service.connection.ConnectionTesterFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceRepository dataSourceRepository;
    private final DataSourceTypeService dataSourceTypeService;
    private final DataSourceHealthService dataSourceHealthService;
    private final ConnectionTesterFactory connectionTesterFactory;

    public DataSourceEntity getDataSource(Long id) {
        return dataSourceRepository.findById(id).orElse(null);
    }

    public Page<DataSourceEntity> listDataSources(int page, int size) {
        return dataSourceRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public DataSourceEntity createDataSource(DataSourceEntity entity) {
        return dataSourceRepository.save(entity);
    }

    @Transactional
    public DataSourceEntity updateDataSource(Long id, DataSourceEntity entity) {
        DataSourceEntity existing = dataSourceRepository.findById(id).orElse(null);
        if (existing == null) return null;
        entity.setId(id);
        entity.setCreatedBy(existing.getCreatedBy());
        entity.setCreatedAt(existing.getCreatedAt());
        return dataSourceRepository.save(entity);
    }

    @Transactional
    public void deleteDataSource(Long id) {
        dataSourceRepository.deleteById(id);
    }

    public ConnectionTestResult testConnection(String typeCode, Map<String, Object> connectionParams) {
        DataSourceTypeEntity type = dataSourceTypeService.getByTypeCode(typeCode);
        if (type == null) {
            return ConnectionTestResult.fail("数据源类型不存在: " + typeCode);
        }
        ConnectionTester tester = connectionTesterFactory.getTester(type.getTestCommand());
        if (tester == null) {
            return ConnectionTestResult.fail("无可用测试器: " + type.getTestCommand());
        }
        Map<String, Object> params = new HashMap<>(connectionParams);
        params.put("_typeCode", typeCode);
        return tester.test(params);
    }

    public ConnectionTestResult testSavedConnection(Long id) {
        DataSourceEntity ds = getDataSource(id);
        if (ds == null) return ConnectionTestResult.fail("数据源不存在");

        DataSourceTypeEntity type = dataSourceTypeService.getByTypeCode(ds.getSourceTypeCode());
        if (type == null) return ConnectionTestResult.fail("数据源类型未配置");

        ConnectionTester tester = connectionTesterFactory.getTester(type.getTestCommand());
        if (tester == null) return ConnectionTestResult.fail("无可用测试器");

        Map<String, Object> params = new HashMap<>();
        if (ds.getConnectionParams() != null) {
            ds.getConnectionParams().fields().forEachRemaining(e ->
                params.put(e.getKey(), e.getValue().isTextual() ? e.getValue().asText() : e.getValue()));
        }
        params.put("_typeCode", ds.getSourceTypeCode());

        ConnectionTestResult result = tester.test(params);

        dataSourceHealthService.recordHealth(id, "MANUAL",
            result.success() ? "SUCCESS" : "FAIL",
            result.success() ? result.latencyMs() : null,
            result.success() ? null : result.message());

        return result;
    }
}
