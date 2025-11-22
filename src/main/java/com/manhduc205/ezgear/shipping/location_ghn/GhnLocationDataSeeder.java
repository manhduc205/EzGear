package com.manhduc205.ezgear.shipping.location_ghn;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GhnLocationDataSeeder implements CommandLineRunner {

    private final GhnLocationSyncService ghnLocationSyncService;
    private final GhnLocationRepository ghnLocationRepository;

    @Override
    public void run(String... args) throws Exception {
        if (ghnLocationRepository.count() == 0) {
            ghnLocationSyncService.syncAll();
        }
    }
}

