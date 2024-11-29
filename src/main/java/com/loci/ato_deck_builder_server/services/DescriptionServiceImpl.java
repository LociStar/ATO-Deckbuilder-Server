package com.loci.ato_deck_builder_server.services;

import com.loci.ato_deck_builder_server.database.objects.Description;
import com.loci.ato_deck_builder_server.database.repositories.DescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DescriptionServiceImpl implements DescriptionService {

    private final DescriptionRepository descriptionRepository;

    @Autowired
    public DescriptionServiceImpl(DescriptionRepository descriptionRepository) {
        this.descriptionRepository = descriptionRepository;
    }

    public Mono<Description> getDescription(String id) {
        return descriptionRepository.findById(id);
    }
}
