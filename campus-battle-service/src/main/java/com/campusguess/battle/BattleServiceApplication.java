package com.campusguess.battle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(scanBasePackages = "com.campusguess")
@EntityScan(basePackages = {"com.campusguess.common.entity", "com.campusguess.battle.entity"})
public class BattleServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BattleServiceApplication.class, args);
    }
}
