package com.isaac.sliceofpie.instrument;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.isaac.sliceofpie.instrument.InstrumentDtos.InstrumentResponse;
import com.isaac.sliceofpie.instrument.InstrumentDtos.ResolveInstrumentRequest;

@RestController
@RequestMapping("/instruments")
public class InstrumentController {

    private final InstrumentResolutionService instrumentResolutionService;

    public InstrumentController(InstrumentResolutionService instrumentResolutionService) {
        this.instrumentResolutionService = instrumentResolutionService;
    }

    /**
     * Resolves a search query (ticker or company name) to an Instrument,
     * creating it if this is the first time it's been requested.
     *
     * This is the entry point watchlist/position/thesis creation should
     * go through - never accept a raw ticker string directly elsewhere.
     */
    @PostMapping("/resolve")
    @ResponseStatus(HttpStatus.OK)
    public InstrumentResponse resolve(@Valid @RequestBody ResolveInstrumentRequest request) {
        Instrument instrument = instrumentResolutionService.resolveOrCreate(request.query());
        return InstrumentResponse.from(instrument);
    }

    @GetMapping("/{id}")
    public InstrumentResponse getById(@PathVariable Long id) {
        return InstrumentResponse.from(instrumentResolutionService.getById(id));
    }
}