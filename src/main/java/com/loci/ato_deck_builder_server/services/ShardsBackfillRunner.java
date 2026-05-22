package com.loci.ato_deck_builder_server.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ShardsBackfillRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ShardsBackfillRunner.class);
    private static final String ENV_FLAG = "SHARDS_BACKFILL";

    private final DeckService deckService;

    public ShardsBackfillRunner(DeckService deckService) {
        this.deckService = deckService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!"true".equalsIgnoreCase(System.getenv(ENV_FLAG))) {
            return;
        }
        logger.info("{}=true detected; running shards backfill", ENV_FLAG);
        Long updated = deckService.backfillShards().block();
        logger.info("Shards backfill finished. Decks updated: {}. Unset {} before restarting.", updated, ENV_FLAG);
    }
}
