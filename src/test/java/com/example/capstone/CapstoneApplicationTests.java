package com.example.capstone;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.capstone.repository.BedConfigStore;
import com.example.capstone.repository.EventStore;
import com.example.capstone.repository.PatientStore;
import com.example.capstone.repository.RoomStore;

@SpringBootTest
@ActiveProfiles("test")
class CapstoneApplicationTests {
	@Autowired
	private BedConfigStore bedConfigStore;

	@Autowired
	private PatientStore patientStore;

	@Autowired
	private EventStore eventStore;

	@Autowired
	private RoomStore roomStore;

	@Test
	void contextLoads() {
	}

	@Test
	void room203ContainsOnlyPatientOne() {
		assertThat(bedConfigStore.findByBedId("B-206")).isPresent();
		assertThat(patientStore.findByPatientNumber("24-2031")).isPresent();
		assertThat(bedConfigStore.findByBedId("B-207")).isEmpty();
		assertThat(patientStore.findByPatientNumber("24-2032")).isEmpty();
		assertThat(eventStore.findRecent("B-207", null, null, 10)).isEmpty();
		assertThat(roomStore.findByRoomId("203호")).hasValueSatisfying(room ->
				assertThat(room.getCapacity()).isEqualTo(1));
	}

}
