package com.campusguess.record;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(scanBasePackages = "com.campusguess")
@EntityScan(basePackages = {"com.campusguess.common.entity", "com.campusguess.record.entity"})
public class RecordServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RecordServiceApplication.class, args);
    }
}
