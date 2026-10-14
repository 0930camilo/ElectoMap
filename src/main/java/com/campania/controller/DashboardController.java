package com.campania.controller;

import com.campania.dto.ApiResponse;
import com.campania.dto.dashboard.AdminDashboardResponse;
import com.campania.dto.dashboard.LiderDashboardResponse;
import com.campania.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

	private final DashboardService dashboardService;

	@GetMapping("/admin")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<AdminDashboardResponse>> dashboardAdmin() {
		return ResponseEntity.ok(ApiResponse.success("Dashboard general consultado correctamente.",
				dashboardService.dashboardAdmin()));
	}

	@GetMapping("/lider")
	@PreAuthorize("hasRole('LIDER')")
	public ResponseEntity<ApiResponse<LiderDashboardResponse>> dashboardLider() {
		return ResponseEntity.ok(ApiResponse.success("Dashboard del líder consultado correctamente.",
				dashboardService.dashboardLider()));
	}
}
