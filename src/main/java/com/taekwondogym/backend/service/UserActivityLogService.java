package com.taekwondogym.backend.service;

import com.taekwondogym.backend.model.UserActivityLog;
import com.taekwondogym.backend.repository.UserActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserActivityLogService {

    @Autowired
    private UserActivityLogRepository activityLogRepository;
    
    @Autowired
    private HttpServletRequest request;

    public void logActivity(String action, String details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth != null && auth.isAuthenticated() ? auth.getName() : "anonymous";
        String ipAddress = extractIpAddress(request);
        
        UserActivityLog log = new UserActivityLog(email, action, ipAddress, details);
        activityLogRepository.save(log);
    }
    
    public Page<UserActivityLog> getUserActivityLogs(String email, Pageable pageable) {
        return activityLogRepository.findByEmail(email, pageable);
    }
    
    public Page<UserActivityLog> getActivityLogsInDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return activityLogRepository.findByTimestampBetween(start, end, pageable);
    }
    
    public Page<UserActivityLog> getActivityLogsByAction(String action, Pageable pageable) {
        return activityLogRepository.findByAction(action, pageable);
    }
    
    public Page<UserActivityLog> getAllActivityLogs(Pageable pageable) {
        return activityLogRepository.findAll(pageable);
    }
    
    // Extracts IP address, handling proxies
    private String extractIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
