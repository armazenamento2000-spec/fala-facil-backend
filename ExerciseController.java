package com.falafacil.controller;

import com.falafacil.dto.ExerciseSubmitRequest;
import com.falafacil.dto.ExerciseResponse;
import com.falafacil.dto.ApiResponse;
import com.falafacil.service.ExerciseService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/exercises")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ExerciseController {

    @Autowired
    private ExerciseService exerciseService;

    @GetMapping
    public ResponseEntity<Page<ExerciseResponse>> listExercises(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(exerciseService.listExercises(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExerciseResponse> getExercise(@PathVariable Long id) {
        return ResponseEntity.ok(exerciseService.getExercise(id));
    }

    @PostMapping("/{id}/submit")
    @RateLimiter(name = "exerciseSubmit")
    public ResponseEntity<ApiResponse> submitExercise(
            @PathVariable Long id,
            @Valid @RequestBody ExerciseSubmitRequest request,
            Authentication authentication) {
        ApiResponse response = exerciseService.submitExercise(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<Page<ExerciseResponse>> getExercisesByCourse(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(exerciseService.getExercisesByCourse(courseId, pageable));
    }
}
