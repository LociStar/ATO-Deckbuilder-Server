package com.loci.ato_deck_builder_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class AtoDeckBuilderServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtoDeckBuilderServerApplication.class, args);
    }

}
