package com.isaac.sliceofpie.thesis;

import com.isaac.sliceofpie.auth.AuthDtos.UserPrincipal;
import com.isaac.sliceofpie.thesis.ThesisDtos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/thesis")
public class ThesisController {

    private final ThesisService service;

    public ThesisController(ThesisService service) {
        this.service = service;
    }

    @PostMapping
    public ThesisResponse upsert(
            @RequestBody UpsertThesisRequest req,
            Authentication auth
    ) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Thesis thesis = service.upsert(principal.id(), req.instrumentId(), req.content());
        return ThesisResponse.from(thesis);
    }

    @GetMapping("/{instrumentId}")
    public ResponseEntity<ThesisResponse> get(
            @PathVariable Long instrumentId,
            Authentication auth
    ) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        return service.getByInstrumentId(principal.id(), instrumentId)
                .map(ThesisResponse::from) // Optional<ThesisResponse>
                .map(ResponseEntity::ok) // Optional<ResponseEntity<ThesisResponse>>
                .orElse(ResponseEntity.noContent().build());
    }
}