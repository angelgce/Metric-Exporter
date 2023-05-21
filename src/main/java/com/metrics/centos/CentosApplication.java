package com.metrics.centos;

import com.metrics.centos.service.ServiceRemoteCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


@SpringBootApplication
public class CentosApplication {


    public static void main(String[] args) {
        SpringApplication.run(CentosApplication.class, args);
    }

}
