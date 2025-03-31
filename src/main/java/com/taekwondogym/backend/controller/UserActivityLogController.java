package com.taekwondogym.backend.controller;

import com.taekwondogym.backend.model.UserActivityLog;
import com.taekwondogym.backend.service.UserActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/activity-logs")
public class UserActivityLogController {
    @Autowired
    private UserActivityLogService activityLogService;

    @GetMapping
    public ResponseEntity<Page<UserActivityLog>> getAllActivityLogs(
            @PageableDefault(size = 10, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(activityLogService.getAllActivityLogs(pageable));
    }
    
    @GetMapping("/user/{email}")
    public ResponseEntity<Page<UserActivityLog>> getUserActivityLogs(
            @PathVariable String email, Pageable pageable) {
        return ResponseEntity.ok(activityLogService.getUserActivityLogs(email, pageable));
    }

    @GetMapping("/date-range")
    public ResponseEntity<Page<UserActivityLog>> getActivityLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Pageable pageable) {
        return ResponseEntity.ok(activityLogService.getActivityLogsInDateRange(start, end, pageable));
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<Page<UserActivityLog>> getActivityLogsByAction(
            @PathVariable String action, Pageable pageable) {
        return ResponseEntity.ok(activityLogService.getActivityLogsByAction(action, pageable));
    }
}
