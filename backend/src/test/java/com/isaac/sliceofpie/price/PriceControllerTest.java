package com.isaac.sliceofpie.price;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.isaac.sliceofpie.instrument.Instrument;
import com.isaac.sliceofpie.instrument.InstrumentResolutionService;
import com.isaac.sliceofpie.prices.PriceController;
import com.isaac.sliceofpie.prices.PriceDtos.PriceResponse;
import com.isaac.sliceofpie.prices.PriceService;
import com.isaac.sliceofpie.prices.exception.PriceExceptionHandler;
import com.isaac.sliceofpie.prices.exception.TickerNotFoundException;
import com.isaac.sliceofpie.prices.lookup.PriceLookupClient;

/**
 * Web-layer test: the real PriceController, PriceService, and
 * PriceExceptionHandler wired together directly (no Spring context, no
 * security, no DB) with only PriceLookupClient mocked.
 *
 * This is the single place PriceService's validation logic (null/negative
 * price -> InvalidPriceException) and PriceExceptionHandler's mapping of
 * that exception to a ProblemDetail are both tested - since the service's
 * exception is exactly what the controller/handler receive, testing them
 * separately would just assert the same behavior twice at different
 * vantage points.
 *
 * TickerNotFoundException itself, as thrown by the real Finnhub client
 * under specific provider failure conditions, stays covered by
 * FinnhubPriceClientTest - here it's just thrown directly by the mock,
 * to isolate "does the app map it correctly" from "does Finnhub trigger it."
 */
@ExtendWith(MockitoExtension.class)
class PriceControllerTest {

    @Mock
    PriceLookupClient priceLookupClient;

    @Mock
    InstrumentResolutionService instrumentResolutionService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PriceService priceService = new PriceService(priceLookupClient, instrumentResolutionService);
        PriceController controller = new PriceController(priceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PriceExceptionHandler())
                .build();
    }

    @Test
    void getPrice_returns200_withPrice() throws Exception {
        Instrument pltr = new Instrument("PLTR", "Palantir", null);
        when(instrumentResolutionService.getById(1L)).thenReturn(pltr);
        when(priceLookupClient.getPrice("PLTR"))
                .thenReturn(new PriceResponse(new BigDecimal("158")));

        mockMvc.perform(get("/price").param("instrumentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(158));
    }

    @Test
    void getPrice_returns502_whenPriceIsNegative() throws Exception {
        // Negative price -> PriceService throws InvalidPriceException.
        // Proves PriceExceptionHandler turns that into a 502 ProblemDetail.
        Instrument pltr = new Instrument("PLTR", "Palantir", null);
        when(instrumentResolutionService.getById(1L)).thenReturn(pltr);
        when(priceLookupClient.getPrice("PLTR"))
                .thenReturn(new PriceResponse(new BigDecimal("-1")));

        mockMvc.perform(get("/price").param("instrumentId", "1"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.detail", Matchers.containsString("PLTR")));
    }

    @Test
    void getPrice_returns502_whenPriceIsNull() throws Exception {
        // Same InvalidPriceException path, triggered by a null price
        // instead of a negative one - the other branch of PriceService's
        // validation.
        Instrument pltr = new Instrument("PLTR", "Palantir", null);
        when(instrumentResolutionService.getById(1L)).thenReturn(pltr);
        when(priceLookupClient.getPrice("PLTR"))
                .thenReturn(new PriceResponse(null));

        mockMvc.perform(get("/price").param("instrumentId", "1"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.detail", Matchers.containsString("PLTR")));
    }

    @Test
    void getPrice_returns502_whenTickerNotFound() throws Exception {
        // Instrument resolves fine (id -> ticker) - it's the downstream
        // price provider that can't find the ticker.
        Instrument missing = new Instrument("MISSING", "Missing Co", null);
        when(instrumentResolutionService.getById(2L)).thenReturn(missing);
        when(priceLookupClient.getPrice("MISSING"))
                .thenThrow(new TickerNotFoundException("Price not found for ticker 'MISSING'"));

        mockMvc.perform(get("/price").param("instrumentId", "2"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.detail", Matchers.containsString("Failed to reach price lookup provider")))
                .andExpect(jsonPath("$.detail", Matchers.containsString("MISSING")));
    }
}
