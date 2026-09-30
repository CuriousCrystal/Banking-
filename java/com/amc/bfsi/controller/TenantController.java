package com.amc.bfsi.controller;

import com.amc.bfsi.dto.TenantResponse;
import com.amc.bfsi.service.TenantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's bank: name, plan, plan limits and how much of them is used. */
@RestController
@RequestMapping("/api/tenant")
public class TenantController {

    private final TenantService service;

    public TenantController(TenantService service) {
        this.service = service;
    }

    @GetMapping
    public TenantResponse current() {
        return service.current();
    }
}
