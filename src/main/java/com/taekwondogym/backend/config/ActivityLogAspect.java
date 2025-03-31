package com.taekwondogym.backend.config;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
@Component
public class ActivityLogAspect {
	private static final Logger log = LoggerFactory.getLogger(ActivityLogAspect.class);
	
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

    @AfterReturning(pointcut = "controllerMethods() && (@annotation(PostMapping) || " +
            "@annotation(PutMapping) || @annotation(DeleteMapping))", returning = "result")
    public void logActivity(JoinPoint joinPoint, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            String httpMethod = request.getMethod(); // More reliable than annotation check
            String path = request.getRequestURI();

            // Safely handle path variables
            String pathVars = "";
            if (signature.getParameterNames() != null) {
                pathVars = IntStream.range(0, joinPoint.getArgs().length)
                    .filter(i -> joinPoint.getArgs()[i] != null)
                    .mapToObj(i -> signature.getParameterNames()[i] + "=" + joinPoint.getArgs()[i])
                    .collect(Collectors.joining(", "));
            }

            // Safely handle request body
            String requestDetails = Arrays.stream(joinPoint.getArgs())
                .filter(arg -> arg != null && !isSimpleValueType(arg.getClass()))
                .map(arg -> {
                    try {
                        return objectMapper.writeValueAsString(arg);
                    } catch (JsonProcessingException e) {
                        return "Unserializable: " + arg.toString();
                    }
                })
                .collect(Collectors.joining(" "));

            String action = httpMethod + " " + path;
            String details = "Params: [" + pathVars + "]" + 
                          (requestDetails.isEmpty() ? "" : " Body: " + requestDetails);
            details = trimLog(details, 1000);

            activityLogService.logActivity(action, details);
        } catch (Exception e) {
            log.error("Activity logging failed", e);
        }
    }

    private boolean isSimpleValueType(Class<?> clazz) {
        return clazz.isPrimitive() || String.class.isAssignableFrom(clazz) ||
               Number.class.isAssignableFrom(clazz) || Boolean.class.isAssignableFrom(clazz) ||
               Pageable.class.isAssignableFrom(clazz);
    }
    
    @AfterThrowing(pointcut = "controllerMethods()", throwing = "exception")
    public void logException(JoinPoint joinPoint, Exception exception) {
        String action = "EXCEPTION " + request.getMethod() + " " + request.getRequestURI();
        String details = "Exception: " + exception.getClass().getName() + " - " + exception.getMessage();
        details = trimLog(details, 1000); 

        activityLogService.logActivity(action, details);
    }
}
