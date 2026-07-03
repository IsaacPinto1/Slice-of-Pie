package com.isaac.sliceofpie.watchlist;

import com.isaac.sliceofpie.auth.AuthTestUtils;
import com.isaac.sliceofpie.instrument.InstrumentRepository;
import com.isaac.sliceofpie.instrument.InstrumentResolutionService;
import com.isaac.sliceofpie.instrument.lookup.InstrumentLookupClient;
import com.isaac.sliceofpie.watchlist.WatchlistDtos.WatchlistItemResponse;
import com.isaac.sliceofpie.watchlist.WatchlistDtos.WatchlistResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@ExtendWith(MockitoExtension.class)

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WatchlistFlowTest {

    private WebTestClient client;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private InstrumentLookupClient instrumentLookupClient;

    private InstrumentResolutionService instrumentResolutionService;

    @BeforeEach
    void setUp(@LocalServerPort int port) {
        instrumentResolutionService = new InstrumentResolutionService(instrumentRepository, instrumentLookupClient);
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void follow_unfollow_and_list_watchlist() {
        String token = AuthTestUtils.registerAndLogin(client, AuthTestUtils.uniqueUsername(), "1234");

        WatchlistItemResponse followed = client.post()
                .uri("/watchlist/AAPL")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WatchlistItemResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(followed);
        assertEquals("AAPL", followed.ticker());

        client.post()
                .uri("/watchlist/TSLA")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        // following the same ticker twice should be a no-op, not a duplicate/error
        client.post()
                .uri("/watchlist/AAPL")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        WatchlistResponse list = client.get()
                .uri("/watchlist")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WatchlistResponse.class)
                .returnResult()
                .getResponseBody();

        System.out.println("LOOK" + list);
        
        List<String> tickers = instrumentResolutionService.getTickersFromIds(list.instrumentIds());

        assertNotNull(list);
        assertEquals(2, list.instrumentIds().size());
        assertTrue(tickers.contains("AAPL"));
        assertTrue(tickers.contains("TSLA"));

        client.delete()
                .uri("/watchlist/AAPL")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();

        // deleting the same ticker twice should be a no-op, not a duplicate/error
        client.delete()
                .uri("/watchlist/AAPL")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();

        WatchlistResponse afterUnfollow = client.get()
                .uri("/watchlist")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WatchlistResponse.class)
                .returnResult()
                .getResponseBody();
        
        List<String> newtickers = instrumentResolutionService.getTickersFromIds(afterUnfollow.instrumentIds());

        assertNotNull(afterUnfollow);
        assertEquals(1, afterUnfollow.instrumentIds().size());
        assertTrue(newtickers.contains("TSLA"));
    }

    @Test
    void watchlist_requires_auth() {
        client.get().uri("/watchlist").exchange().expectStatus().isUnauthorized();
        client.post().uri("/watchlist/AAPL").exchange().expectStatus().isUnauthorized();
        client.delete().uri("/watchlist/AAPL").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void users_only_see_their_own_watchlist() {
        String tokenA = AuthTestUtils.registerAndLogin(client, AuthTestUtils.uniqueUsername(), "1234");
        String tokenB = AuthTestUtils.registerAndLogin(client, AuthTestUtils.uniqueUsername(), "1234");

        client.post()
                .uri("/watchlist/AAPL")
                .header("Authorization", "Bearer " + tokenA)
                .exchange()
                .expectStatus().isOk();

        WatchlistResponse userBList = client.get()
                .uri("/watchlist")
                .header("Authorization", "Bearer " + tokenB)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WatchlistResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(userBList);
        assertTrue(userBList.instrumentIds().isEmpty());
    }
}