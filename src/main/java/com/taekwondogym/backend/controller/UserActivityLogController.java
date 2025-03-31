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
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime end,
        Pageable pageable) {

    ZoneId bangkokZone = ZoneId.of("Asia/Bangkok");
    ZonedDateTime startInBangkok = start.withZoneSameInstant(bangkokZone);
    ZonedDateTime endInBangkok = end.withZoneSameInstant(bangkokZone);

    return ResponseEntity.ok(activityLogService.getActivityLogsInDateRange(startInBangkok, endInBangkok, pageable));
}

    @GetMapping("/action/{action}")
    public ResponseEntity<Page<UserActivityLog>> getActivityLogsByAction(
            @PathVariable String action, Pageable pageable) {
        return ResponseEntity.ok(activityLogService.getActivityLogsByAction(action, pageable));
    }
}
