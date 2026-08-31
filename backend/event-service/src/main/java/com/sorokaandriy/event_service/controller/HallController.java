package com.sorokaandriy.event_service.controller;


import com.sorokaandriy.event_service.dto.requests.HallRequest;
import com.sorokaandriy.event_service.dto.responses.HallResponse;
import com.sorokaandriy.event_service.service.HallService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/halls")
@RequiredArgsConstructor
public class HallController {

    private final HallService service;

    @GetMapping("/{id}")
    public ResponseEntity<HallResponse> findHallById(
            @PathVariable UUID id
    ){
        return ResponseEntity.ok(service.findHallById(id));
    }


    @PutMapping("/{id}")
    public ResponseEntity<HallResponse> updateHall(
            @PathVariable UUID id,
            @RequestBody HallRequest request
            ){
        return ResponseEntity.ok(service.updateHall(id,request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHall(
            @PathVariable UUID id
    )
    {
        service.deleteHall(id);
        return ResponseEntity.noContent().build();
    }

}
