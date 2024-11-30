package com.loci.ato_deck_builder_server.services;

import com.loci.ato_deck_builder_server.database.objects.Description;
import com.loci.ato_deck_builder_server.database.repositories.DescriptionRepository;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class DescriptionServiceImpl implements DescriptionService {

    private final AsyncLoadingCache<String, Description> descriptionCache;

    @Autowired
    public DescriptionServiceImpl(DescriptionRepository descriptionRepository) {

        this.descriptionCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(10, TimeUnit.DAYS)
                .buildAsync((id, executor) -> descriptionRepository.findById(id).toFuture());
    }

    public Mono<Description> getDescription(String id) {
        CompletableFuture<Description> descriptionFuture = descriptionCache.get(id);
        return Mono.fromFuture(descriptionFuture);
    }
}
