package com.isaac.sliceofpie.instrument;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.isaac.sliceofpie.instrument.InstrumentDtos.CreateInstrumentRequest;
import com.isaac.sliceofpie.instrument.InstrumentDtos.InstrumentResponse;
import com.isaac.sliceofpie.instrument.InstrumentDtos.InstrumentSearchResult;

@RestController
@RequestMapping("/instruments")
public class InstrumentController {

    private final InstrumentResolutionService instrumentResolutionService;

    public InstrumentController(InstrumentResolutionService instrumentResolutionService) {
        this.instrumentResolutionService = instrumentResolutionService;
    }

    /**
     * Search-as-you-type lookup for the watchlist "add" dropdown. Read-only -
     * proxies the lookup provider's candidates (capped to 5), never creates
     * an Instrument. The frontend debounces calls to this endpoint.
     */
    @GetMapping("/search")
    public List<InstrumentSearchResult> search(@RequestParam("q") String query) {
        return instrumentResolutionService.search(query);
    }

    /**
     * Creates an Instrument from a result the user explicitly selected out
     * of the search dropdown. This is the ONLY way an Instrument comes into
     * existence - there is no create-on-the-fly path anymore. Idempotent:
     * selecting a ticker that already exists just returns the existing row.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstrumentResponse create(@Valid @RequestBody CreateInstrumentRequest request) {
        Instrument instrument = instrumentResolutionService.create(request.ticker(), request.name());
        return InstrumentResponse.from(instrument);
    }

    @GetMapping("/{id}")
    public InstrumentResponse getById(@PathVariable Long id) {
        return InstrumentResponse.from(instrumentResolutionService.getById(id));
    }
}