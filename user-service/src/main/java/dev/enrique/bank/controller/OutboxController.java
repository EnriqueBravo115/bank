package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.OUTBOX;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.enrique.bank.broker.outbox.EventOutbox;
import dev.enrique.bank.commons.constants.Paging;
import dev.enrique.bank.commons.dto.response.OutboxEventResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

// Events that ran out of publish attempts: an admin checks lastError and puts them back in the queue.
@RestController
@RequestMapping(OUTBOX)
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class OutboxController {
    private final EventOutbox eventOutbox;

    @GetMapping("/failed")
    public ResponseEntity<PagedModel<OutboxEventResponse>> getFailedEvents(
            @RequestParam(defaultValue = Paging.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = Paging.DEFAULT_SIZE) @Min(1) @Max(Paging.MAX_SIZE) int size) {
        return ResponseEntity.ok(new PagedModel<>(eventOutbox.findFailed(PageRequest.of(page, size))));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<OutboxEventResponse> retry(@PathVariable UUID id) {
        return ResponseEntity.ok(eventOutbox.retry(id));
    }
}
