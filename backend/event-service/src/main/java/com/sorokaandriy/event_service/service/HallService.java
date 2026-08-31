package com.sorokaandriy.event_service.service;

import com.sorokaandriy.event_service.dto.requests.HallRequest;
import com.sorokaandriy.event_service.dto.responses.HallResponse;
import com.sorokaandriy.event_service.dto.responses.SeatResponse;
import com.sorokaandriy.event_service.entity.Hall;
import com.sorokaandriy.event_service.entity.Seat;
import com.sorokaandriy.event_service.entity.Venue;
import com.sorokaandriy.event_service.exception.HallNotFoundException;
import com.sorokaandriy.event_service.exception.SeatsAlreadyGeneratedException;
import com.sorokaandriy.event_service.exception.VenueNotFoundException;
import com.sorokaandriy.event_service.repository.HallRepository;
import com.sorokaandriy.event_service.repository.SeatRepository;
import com.sorokaandriy.event_service.repository.VenueRepository;
import com.sorokaandriy.event_service.service.mapper.HallMapper;
import com.sorokaandriy.event_service.service.mapper.SeatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class HallService {

    private final VenueRepository venueRepository;
    private final HallRepository hallRepository;
    private final HallMapper hallMapper;
    private final SeatRepository seatRepository;
    private final SeatMapper seatMapper;


    public HallResponse createHall(UUID id, HallRequest request) {

        Hall hall = hallMapper.fromHallRequestToHall(request);

        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new VenueNotFoundException("Venue with id " + id + " not found"));
        log.info("Get venue from db {}", venue.getId());

        hall.setVenue(venue);
        hallRepository.save(hall);
        log.info("Save hall to db {}", hall.getId());

        return hallMapper.fromHallToHallResponse(hall);

    }

    public List<HallResponse> findHallsByVenueId(UUID venuId) {

        Venue venue = venueRepository.findById(venuId)
                .orElseThrow(() -> new VenueNotFoundException("Venue with id " + venuId + " not found"));
        log.info("Get venue from db {}", venue.getId());

        return venue.getHalls().stream().map(hall -> hallMapper.fromHallToHallResponse(hall)).toList();

    }


    public HallResponse findHallById(UUID id) {

        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new HallNotFoundException("Hall with id " + id + " not found"));
        log.info("Get hall from db {}", id);

        return hallMapper.fromHallToHallResponse(hall);
    }


    public HallResponse updateHall(UUID id, HallRequest request) {

        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new HallNotFoundException("Hall with id " + id + " not found"));
        log.info("Get hall from db {}", id);

        hall.setName(request.name());
        hall.setRowsCount(request.rowsCount());
        hall.setSeatsPerRow(request.seatsPerRow());
        hall.setUpdatedAt(Instant.now());
        hallRepository.save(hall);

        return hallMapper.fromHallToHallResponse(hall);
    }

    public void deleteHall(UUID id) {

        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new HallNotFoundException("Hall with id " + id + " not found"));
        log.info("Get hall from db {}", id);

        hallRepository.delete(hall);
    }


    @Transactional
    public List<SeatResponse> generateSeats(UUID hallId) {

        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new HallNotFoundException("Hall with id " + hallId + " not found"));
        log.info("Get hall from db {}", hallId);

        if (seatRepository.existsByHallId(hallId)) {
            throw new SeatsAlreadyGeneratedException("Seats already exist");
        }


        List<Seat> seats = new ArrayList<>();
        int rowsCount = hall.getRowsCount();
        int seatsPerRow = hall.getSeatsPerRow();

        for (int row = 1; row <= rowsCount; row++){
            for (int seat = 1; seat <= seatsPerRow; seat++){
                seats.add(Seat.builder()
                        .rowNumber(row)
                        .seatNumber(seat)
                        .sector(determineSector(row, hall.getRowsCount()))
                        .hall(hall)
                        .build());
            }
        }

        List<Seat> saved = seatRepository.saveAll(seats);
        return saved.stream()
                .map(seat -> seatMapper.fromSeatToSeatResponse(seat))
                .toList();
    }


    private String determineSector(int row, int totalRows) {
        if (row <= totalRows / 3) return "VIP";
        if (row <= 2 * totalRows / 3) return "A";
        return "B";
    }


    public Page<SeatResponse> findSeatsByHallId(UUID hallId, int page, int size, String sortBy) {

        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new HallNotFoundException("Hall with id " + hallId + " not found"));
        log.info("Get hall from db {}", hallId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));

        return seatRepository.findAllByHall(hall, pageable)
                .map(seat -> seatMapper.fromSeatToSeatResponse(seat));

    }
}
