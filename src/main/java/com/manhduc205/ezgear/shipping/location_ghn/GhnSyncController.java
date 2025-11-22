package com.manhduc205.ezgear.shipping.location_ghn;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ghn-sync")
@RequiredArgsConstructor
public class GhnSyncController {

    private final GhnLocationSyncService syncService;

    @PostMapping("/run")
    public String syncAll() {
        syncService.syncAll();
        return "GHN locations synced successfully!";
    }
}

