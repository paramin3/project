package com.taekwondogym.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taekwondogym.backend.service.UserActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

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

    private String trimLog(String input, int maxLength) {
        if (input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength) + "...[truncated]";
    }

    @Pointcut("execution(* com.taekwondogym.backend.controller.*.*(..))")
    public void controllerMethods() {}

    @AfterReturning(pointcut = "controllerMethods() && (@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping))", returning = "result")
    public void logActivity(JoinPoint joinPoint, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            String httpMethod = "";
            if (method.isAnnotationPresent(PostMapping.class)) {
                httpMethod = "POST";
            } else if (method.isAnnotationPresent(PutMapping.class)) {
                httpMethod = "PUT";
            } else if (method.isAnnotationPresent(DeleteMapping.class)) {
                httpMethod = "DELETE";
            }

            String path = request.getRequestURI();

            String pathVars = Arrays.stream(signature.getParameterNames())
                    .filter(param -> joinPoint.getArgs()[Arrays.asList(signature.getParameterNames()).indexOf(param)] != null)
                    .map(param -> param + "=" + joinPoint.getArgs()[Arrays.asList(signature.getParameterNames()).indexOf(param)])
                    .collect(Collectors.joining(", "));

            String requestDetails = "";
            try {
                Object[] args = joinPoint.getArgs();
                for (Object arg : args) {
                    if (arg != null && !arg.getClass().isPrimitive() && !(arg instanceof String)
                            && !(arg instanceof Number) && !(arg instanceof Boolean) && !(arg instanceof Pageable)) {
                        requestDetails += objectMapper.writeValueAsString(arg) + " ";
                    }
                }
            } catch (Exception e) {
                requestDetails = "Failed to serialize request body: " + e.getMessage();
            }

            String action = httpMethod + " " + path;
            String details = "Path Variables: [" + pathVars + "], Body: " + requestDetails.trim();
            details = trimLog(details, 1000); 

            activityLogService.logActivity(action, details);

        } catch (Exception e) {
            System.err.println("Error in activity logging: " + e.getMessage());
        }
    }

    @AfterThrowing(pointcut = "controllerMethods()", throwing = "exception")
    public void logException(JoinPoint joinPoint, Exception exception) {
        String action = "EXCEPTION " + request.getMethod() + " " + request.getRequestURI();
        String details = "Exception: " + exception.getClass().getName() + " - " + exception.getMessage();
        details = trimLog(details, 1000); 

        activityLogService.logActivity(action, details);
    }
}
