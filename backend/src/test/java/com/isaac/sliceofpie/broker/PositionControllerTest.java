package com.isaac.sliceofpie.broker;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.isaac.sliceofpie.auth.AuthDtos.UserPrincipal;
import com.isaac.sliceofpie.broker.PositionDtos.BrokerHolding;
import com.isaac.sliceofpie.broker.exception.BrokerExceptionHandler;
import com.isaac.sliceofpie.broker.exception.BrokerLookupException;
import com.isaac.sliceofpie.broker.exception.RequestSigningException;
import com.isaac.sliceofpie.broker.lookup.BrokerClient;
import com.isaac.sliceofpie.instrument.Instrument;
import com.isaac.sliceofpie.instrument.InstrumentResolutionService;

/**
 * Web-layer test: the real PositionController, PositionService,
 * BrokerAccessGuard, and BrokerExceptionHandler wired together directly (no
 * Spring context, no security filter chain, no DB) with only BrokerClient,
 * InstrumentResolutionService, and PositionRepository mocked. Mirrors
 * PriceControllerTest's shape.
 *
 * This is the place PositionService's exception handling (not connected,
 * lookup failure, signing failure) and BrokerExceptionHandler's mapping of
 * each to a ProblemDetail are both exercised, along with
 * BrokerAccessGuard's allowlist gate actually running in front of the
 * controller methods. PositionFlowTest additionally proves the whole thing
 * is wired up correctly behind the real security filter chain and JWT
 * auth, and that hitting these routes as a non-allowed user really is
 * indistinguishable from a 404.
 */
@ExtendWith(MockitoExtension.class)
class PositionControllerTest {

    private static final Long USER_ID = 1L;
    private static final String ALLOWED_USERNAME = "alloweduser";

    @Mock
    BrokerClient brokerClient;

    @Mock
    InstrumentResolutionService instrumentResolutionService;

    @Mock
    PositionRepository positionRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PositionService positionService =
                new PositionService(brokerClient, instrumentResolutionService, positionRepository);
        BrokerAccessGuard brokerAccessGuard = new BrokerAccessGuard(ALLOWED_USERNAME);
        PositionController controller = new PositionController(positionService, brokerAccessGuard);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new BrokerExceptionHandler())
                .build();
    }

    private Authentication allowedUser() {
        return new TestingAuthenticationToken(new UserPrincipal(USER_ID, ALLOWED_USERNAME), null);
    }

    private Authentication disallowedUser() {
        return new TestingAuthenticationToken(new UserPrincipal(2L, "someoneelse"), null);
    }

    @Test
    void status_returns200_withConnectedTrue() throws Exception {
        when(brokerClient.hasConnectedAccounts()).thenReturn(true);

        mockMvc.perform(get("/broker/status").principal(allowedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true));
    }

    @Test
    void status_returns200_withConnectedFalse() throws Exception {
        when(brokerClient.hasConnectedAccounts()).thenReturn(false);

        mockMvc.perform(get("/broker/status").principal(allowedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false));
    }

    @Test
    void status_returns404_whenUserNotAllowed() throws Exception {
        // Deliberately plain 404, not 403 - see BrokerAccessDeniedException.
        mockMvc.perform(get("/broker/status").principal(disallowedUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verifyNoInteractions(brokerClient);
    }

    @Test
    void getPositions_returns200_withStoredPositions() throws Exception {
        Instrument aapl = new Instrument("AAPL", "Apple Inc", null);
        Position position = new Position(USER_ID, aapl, new BigDecimal("5"));
        when(positionRepository.findAllByUserIdFetchInstrument(USER_ID)).thenReturn(List.of(position));

        mockMvc.perform(get("/positions").principal(allowedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$.items[0].quantity").value(5));
    }

    @Test
    void getPositions_returns404_whenUserNotAllowed() throws Exception {
        mockMvc.perform(get("/positions").principal(disallowedUser()))
                .andExpect(status().isNotFound());

        verifyNoInteractions(positionRepository);
    }

    @Test
    void sync_returns200_withReconciledPositions() throws Exception {
        Instrument aapl = new Instrument("AAPL", "Apple Inc", null);
        when(brokerClient.hasConnectedAccounts()).thenReturn(true);
        when(brokerClient.fetchHoldings())
                .thenReturn(List.of(new BrokerHolding("AAPL", "Apple Inc", new BigDecimal("5"))));
        when(instrumentResolutionService.resolve("AAPL")).thenReturn(aapl);
        when(positionRepository.findByUserIdAndInstrumentId(eq(USER_ID), any())).thenReturn(Optional.empty());
        when(positionRepository.findAllByUserIdFetchInstrument(USER_ID))
                .thenReturn(List.of(new Position(USER_ID, aapl, new BigDecimal("5"))));

        mockMvc.perform(post("/positions/sync").principal(allowedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$.items[0].quantity").value(5));
    }

    @Test
    void sync_returns409_whenProviderReportsNoConnections() throws Exception {
        // BrokerNotConnectedException -> 409, per BrokerExceptionHandler.
        when(brokerClient.hasConnectedAccounts()).thenReturn(false);

        mockMvc.perform(post("/positions/sync").principal(allowedUser()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("No brokerage accounts connected")));

        verifyNoInteractions(positionRepository);
    }

    @Test
    void sync_returns404_whenUserNotAllowed() throws Exception {
        mockMvc.perform(post("/positions/sync").principal(disallowedUser()))
                .andExpect(status().isNotFound());

        verifyNoInteractions(brokerClient);
    }

    @Test
    void sync_returns502_whenBrokerLookupFails() throws Exception {
        // BrokerLookupException (a failed SnapTrade call) -> 502.
        when(brokerClient.hasConnectedAccounts()).thenReturn(true);
        when(brokerClient.fetchHoldings())
                .thenThrow(new BrokerLookupException("SnapTrade GET request failed: /accounts", new RuntimeException("boom")));

        mockMvc.perform(post("/positions/sync").principal(allowedUser()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail", Matchers.containsString("Failed to reach brokerage provider")));
    }

    @Test
    void sync_returns500_whenRequestSigningFails() throws Exception {
        // RequestSigningException (our own fault, not the provider's) ->
        // 500, with a generic detail message that never echoes
        // ex.getMessage() - see BrokerExceptionHandler's comment.
        when(brokerClient.hasConnectedAccounts()).thenReturn(true);
        when(brokerClient.fetchHoldings())
                .thenThrow(new RequestSigningException("key material leaked here", new RuntimeException("boom")));

        mockMvc.perform(post("/positions/sync").principal(allowedUser()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Failed to sign outgoing request"));
    }
}
