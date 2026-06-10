package com.example.capstone.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capstone.dto.QuestionRequest;
import com.example.capstone.dto.QuestionResponse;
import com.example.capstone.service.QuestionService;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

	private final QuestionService questionService;

	public QuestionController(QuestionService questionService) {
		this.questionService = questionService;
	}

	@PostMapping
	public QuestionResponse ask(@RequestBody QuestionRequest request) {
		return new QuestionResponse(questionService.answer(request.question(), request.bedId()));
	}
}
