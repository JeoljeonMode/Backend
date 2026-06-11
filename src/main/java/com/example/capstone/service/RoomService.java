package com.example.capstone.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.capstone.domain.BedConfig;
import com.example.capstone.domain.Room;
import com.example.capstone.dto.RoomRequest;
import com.example.capstone.dto.RoomResponse;
import com.example.capstone.repository.BedConfigStore;
import com.example.capstone.repository.RoomStore;

@Service
public class RoomService {

    private final RoomStore roomStore;
    private final BedConfigStore bedConfigStore;

    public RoomService(RoomStore roomStore, BedConfigStore bedConfigStore) {
        this.roomStore = roomStore;
        this.bedConfigStore = bedConfigStore;
    }

    public List<RoomResponse> getAllRooms() {
        List<BedConfig> beds = bedConfigStore.findAll();
        return roomStore.findAllByOrderByRoomIdAsc().stream()
                .map(room -> toResponse(room, beds))
                .toList();
    }

    public RoomResponse createRoom(RoomRequest request) {
        validateRoomRequest(request);
        if (roomStore.existsByRoomId(request.roomId())) {
            throw new IllegalArgumentException("이미 존재하는 병실 ID입니다.");
        }
        if (roomStore.existsByCameraId(request.cameraId())) {
            throw new IllegalArgumentException("이미 사용 중인 카메라 ID입니다.");
        }

        Room room = new Room();
        room.setRoomId(request.roomId());
        room.setLabel(request.label());
        room.setCameraId(request.cameraId());
        room.setGender(request.gender());
        room.setCapacity(request.capacity());
        return toResponse(roomStore.save(room), List.of());
    }

    private void validateRoomRequest(RoomRequest request) {
        if (request.roomId() == null || request.roomId().isBlank()) {
            throw new IllegalArgumentException("병실 ID를 입력해 주세요.");
        }
        if (request.label() == null || request.label().isBlank()) {
            throw new IllegalArgumentException("병실명을 입력해 주세요.");
        }
        if (request.cameraId() == null || request.cameraId().isBlank()) {
            throw new IllegalArgumentException("카메라 ID를 입력해 주세요.");
        }
        if (request.capacity() < 1) {
            throw new IllegalArgumentException("정원은 1명 이상이어야 합니다.");
        }
    }

    private RoomResponse toResponse(Room room, List<BedConfig> beds) {
        List<String> bedIds = beds.stream()
                .filter(bed -> room.getRoomId().equals(bed.getRoomId()))
                .map(BedConfig::getBedId)
                .sorted(Comparator.naturalOrder())
                .toList();
        return new RoomResponse(room.getRoomId(), room.getLabel(), room.getCameraId(),
                room.getGender(), room.getCapacity(), bedIds);
    }
}
