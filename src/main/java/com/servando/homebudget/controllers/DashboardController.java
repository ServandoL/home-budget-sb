package com.servando.homebudget.controllers;

import com.home.household.CurrentMember;
import com.servando.homebudget.models.dto.DashboardDto;
import com.servando.homebudget.services.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/dashboard")
@Controller
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardDto> dashboard(
            CurrentMember member,
            @RequestParam(required = false) Double targetMonthlyTransfer,
            @RequestParam(defaultValue = "0") Double payPeriodBuffer
    ) {
        var result = dashboardService.getDashboard(member.householdId(), targetMonthlyTransfer, payPeriodBuffer);
        return ResponseEntity.ok(result);
    }
}
