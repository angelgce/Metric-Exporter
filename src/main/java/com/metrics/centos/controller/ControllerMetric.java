package com.metrics.centos.controller;

import com.metrics.centos.exceptions.BadRequestException;
import com.metrics.centos.service.ServiceRemoteCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
public class ControllerMetric {

    private final ServiceRemoteCommand serviceRemoteCommand;

    @GetMapping
    public ResponseEntity getMetric() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(serviceRemoteCommand.getPrometheusMetrics().trim());
    }
}
