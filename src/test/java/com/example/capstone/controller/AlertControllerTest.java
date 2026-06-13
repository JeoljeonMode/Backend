package com.example.capstone.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AlertControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void latestAlertReturnsNotFoundWhenNoAlertHasBeenReceived() throws Exception {
		mockMvc.perform(get("/api/alerts/latest")
						.with(user("admin").roles("ADMIN")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("No alert has been received yet"));
	}
}
