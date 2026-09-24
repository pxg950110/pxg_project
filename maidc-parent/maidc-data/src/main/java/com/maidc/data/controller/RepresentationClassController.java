package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.common.security.annotation.RequirePermission;
import com.maidc.data.entity.RepresentationClassEntity;
import com.maidc.data.service.RepresentationClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/representation-classes")
@RequiredArgsConstructor
public class RepresentationClassController {

    private final RepresentationClassService service;

    @RequirePermission("masterdata:read")
    @GetMapping
    public R<List<RepresentationClassEntity>> list() {
        return R.ok(service.listActive());
    }
}
