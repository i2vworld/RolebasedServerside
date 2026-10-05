package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.response.ExitResponseDto;
import com.world.i2v.i2vauthapi.entity.ClearanceStep;
import com.world.i2v.i2vauthapi.entity.ExitRequest;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.ClearanceStepRepository;
import com.world.i2v.i2vauthapi.repository.ExitRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExitManagementService {

    private final ExitRequestRepository exitRequestRepository;
    private final ClearanceStepRepository clearanceStepRepository;
    private final ExitService exitService; // reuse mapping

    @Transactional(readOnly = true)
    public List<ExitResponseDto> getAllExitRequests(String statusFilter) {
        List<ExitRequest> requests = exitRequestRepository.findAllByOrderByAppliedAtDesc();

        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                ExitRequest.ExitStatus filter = ExitRequest.ExitStatus.valueOf(statusFilter.toUpperCase());
                requests = requests.stream()
                        .filter(r -> r.getStatus() == filter)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {
                // ignore invalid status filter, return all
            }
        }
        return requests.stream().map(exitService::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public void approveStep(Long exitRequestId, Long stepId) {
        ClearanceStep step = clearanceStepRepository.findById(stepId)
                .orElseThrow(() -> new ResourceNotFoundException("Step not found"));
        if (!step.getExitRequest().getId().equals(exitRequestId)) {
            throw new BadRequestException("Step does not belong to the given exit request");
        }
        if (step.getStatus() != ExitRequest.ExitStatus.PENDING) {
            throw new BadRequestException("Step is not in pending state");
        }

        step.setStatus(ExitRequest.ExitStatus.APPROVED);
        step.setCompletedAt(LocalDateTime.now());
        clearanceStepRepository.save(step);

        updateOverallStatus(step.getExitRequest());
        log.info("Step {} approved for exit request {}", stepId, exitRequestId);
    }

    @Transactional
    public void rejectStep(Long exitRequestId, Long stepId, String remarks) {
        ClearanceStep step = clearanceStepRepository.findById(stepId)
                .orElseThrow(() -> new ResourceNotFoundException("Step not found"));
        if (!step.getExitRequest().getId().equals(exitRequestId)) {
            throw new BadRequestException("Step does not belong to the given exit request");
        }
        if (step.getStatus() != ExitRequest.ExitStatus.PENDING) {
            throw new BadRequestException("Step is not in pending state");
        }

        step.setStatus(ExitRequest.ExitStatus.REJECTED);
        step.setCompletedAt(LocalDateTime.now());
        // optionally store remarks in a field if you add one, otherwise log
        clearanceStepRepository.save(step);

        // If one step rejected, the whole request is rejected
        ExitRequest request = step.getExitRequest();
        request.setStatus(ExitRequest.ExitStatus.REJECTED);
        request.setUpdatedAt(LocalDateTime.now());
        exitRequestRepository.save(request);

        log.info("Step {} rejected for exit request {}. Request rejected.", stepId, exitRequestId);
    }

    @Transactional
    public void reopenExitRequest(Long exitRequestId) {
        ExitRequest request = exitRequestRepository.findById(exitRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Exit request not found"));
        exitService.reopenRejectedRequest(request);
        exitRequestRepository.save(request);
        log.info("Admin reopened exit request {}", exitRequestId);
    }

    @Transactional
    public void hardDeleteExitRequest(Long exitRequestId) {
        ExitRequest request = exitRequestRepository.findById(exitRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Exit request not found"));
        exitService.validateRejectedRequest(request);
        exitRequestRepository.delete(request);
        log.info("Admin hard deleted exit request {}", exitRequestId);
    }

    private void updateOverallStatus(ExitRequest exitRequest) {
        boolean allApproved = exitRequest.getClearanceSteps().stream()
                .allMatch(step -> step.getStatus() == ExitRequest.ExitStatus.APPROVED);
        if (allApproved) {
            exitRequest.setStatus(ExitRequest.ExitStatus.APPROVED);
            exitRequest.setUpdatedAt(LocalDateTime.now());
            exitRequestRepository.save(exitRequest);
        }
        // If not all approved yet, keep PENDING (rejection handled separately)
    }
}
