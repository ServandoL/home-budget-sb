package com.servando.homebudget.controllers;

import com.home.household.CurrentMember;
import com.servando.homebudget.models.dto.DashboardDto;
import com.servando.homebudget.services.BudgetSettingsService;
import com.servando.homebudget.services.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/dashboard")
@Controller
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private BudgetSettingsService budgetSettings;

    @GetMapping
    public ResponseEntity<DashboardDto> dashboard(CurrentMember member) {
        // The household's saved settings, the same on every device.
        var settings = budgetSettings.get(member.householdId());
        var result = dashboardService.getDashboard(member.householdId(), settings.targetMonthlyTransfer(),
                settings.payPeriodBuffer());
        return ResponseEntity.ok(result);
    }
}
