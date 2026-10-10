package org.altechego.leavewise;

import org.springframework.boot.SpringApplication;

public class TestLeavewiseApplication {

    public static void main(String[] args) {
        SpringApplication.from(LeavewiseApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
