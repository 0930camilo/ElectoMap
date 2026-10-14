package com.campania.service;

import com.campania.dto.dashboard.AdminDashboardResponse;
import com.campania.dto.dashboard.LiderDashboardResponse;

public interface DashboardService {

	AdminDashboardResponse dashboardAdmin();

	LiderDashboardResponse dashboardLider();
}
