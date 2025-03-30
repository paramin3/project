package com.taekwondogym.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taekwondogym.backend.service.UserActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
public class ActivityLogAspect {

    @Autowired
    private UserActivityLogService activityLogService;

    @Autowired
    private HttpServletRequest request;
    
    @Autowired
    private ObjectMapper objectMapper;

    // Pointcut for controller methods
    @Pointcut("execution(* com.taekwondogym.backend.controller.*.*(..))")
    public void controllerMethods() {}

    // Log POST, PUT, DELETE methods
    @AfterReturning(pointcut = "controllerMethods() && (@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
                             "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
                             "@annotation(org.springframework.web.bind.annotation.DeleteMapping))",
                    returning = "result")
    public void logActivity(JoinPoint joinPoint, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            
            // Determine HTTP method type
            String httpMethod = "";
            if (method.isAnnotationPresent(PostMapping.class)) {
                httpMethod = "POST";
            } else if (method.isAnnotationPresent(PutMapping.class)) {
                httpMethod = "PUT";
            } else if (method.isAnnotationPresent(DeleteMapping.class)) {
                httpMethod = "DELETE";
            }
            
            // Get request path
            String path = request.getRequestURI();
            
            // Extract path variables
            String pathVars = Arrays.stream(signature.getParameterNames())
                .filter(param -> joinPoint.getArgs()[Arrays.asList(signature.getParameterNames()).indexOf(param)] != null)
                .map(param -> param + "=" + joinPoint.getArgs()[Arrays.asList(signature.getParameterNames()).indexOf(param)])
                .collect(Collectors.joining(", "));
                
            // Get request body or params
            String requestDetails = "";
            try {
                Object[] args = joinPoint.getArgs();
                for (Object arg : args) {
                    // Skip primitive types and strings which are likely path variables
                    if (arg != null && !arg.getClass().isPrimitive() && !(arg instanceof String)
                            && !(arg instanceof Number) && !(arg instanceof Boolean) && !(arg instanceof Pageable)) {
                        requestDetails += objectMapper.writeValueAsString(arg) + " ";
                    }
                }
            } catch (Exception e) {
                requestDetails = "Failed to serialize request body: " + e.getMessage();
            }
            
            // Build action and details
            String action = httpMethod + " " + path;
           String details = "Path Variables: [" + pathVars + "], Body: " + requestDetails.trim();
if (details.length() > 1000) {
    details = details.substring(0, 1000) + "...[truncated]";
}
            
            // Log the activity
            activityLogService.logActivity(action, details);
            
        } catch (Exception e) {
            // Log the logging error but don't disrupt the main flow
            System.err.println("Error in activity logging: " + e.getMessage());
        }
    }
    
    // Log exceptions in controller methods
    @AfterThrowing(pointcut = "controllerMethods()", throwing = "exception")
    public void logException(JoinPoint joinPoint, Exception exception) {
        String action = "EXCEPTION " + request.getMethod() + " " + request.getRequestURI();
        String details = "Exception: " + exception.getClass().getName() + " - " + exception.getMessage();
        
        activityLogService.logActivity(action, details);
    }
}
