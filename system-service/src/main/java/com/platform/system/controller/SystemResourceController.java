package com.platform.system.controller;

import com.platform.common.auth.RequirePermission;
import com.platform.common.result.R;
import com.platform.system.resource.ResourceSnapshot;
import com.platform.system.resource.SystemResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/resources")
@RequirePermission("SYSTEM_RESOURCE_VIEW")
@RequiredArgsConstructor
public class SystemResourceController {
    private final SystemResourceService resources;
    @GetMapping
    public R<ResourceSnapshot> snapshot() { return R.ok(resources.snapshot()); }
}
