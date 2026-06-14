package com.example.capstone.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.capstone.domain.Room;

public interface RoomStore extends JpaRepository<Room, String> {
    Optional<Room> findByRoomId(String roomId);
    Optional<Room> findByCameraId(String cameraId);
    boolean existsByRoomId(String roomId);
    boolean existsByCameraId(String cameraId);
    List<Room> findAllByOrderByRoomIdAsc();
}
