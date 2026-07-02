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
    public ResponseEntity<ThesisResponse> upsert(
            @RequestBody UpsertThesisRequest req,
            Authentication auth
    ) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Thesis thesis = service.upsert(principal.id(), req.ticker(), req.content());
        return ResponseEntity.ok(
                new ThesisResponse(
                        thesis.getTicker(),
                        thesis.getContent(),
                        thesis.getCreatedAt(),
                        thesis.getUpdatedAt()
                )
        );
    }

    @GetMapping("/{ticker}")
    public ResponseEntity<ThesisResponse> get(
            @PathVariable String ticker,
            Authentication auth
    ) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Thesis thesis = service.get(principal.id(), ticker);

        return ResponseEntity.ok(
                new ThesisResponse(
                        thesis.getTicker(),
                        thesis.getContent(),
                        thesis.getCreatedAt(),
                        thesis.getUpdatedAt()
                )
        );
    }
}