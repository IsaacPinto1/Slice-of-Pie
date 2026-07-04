package com.isaac.sliceofpie.watchlist;

import com.isaac.sliceofpie.auth.AuthTestUtils;
import com.isaac.sliceofpie.instrument.InstrumentDtos.InstrumentSearchResult;
import com.isaac.sliceofpie.instrument.lookup.InstrumentLookupClient;
import com.isaac.sliceofpie.watchlist.WatchlistDtos.WatchlistItemResponse;
import com.isaac.sliceofpie.watchlist.WatchlistDtos.WatchlistResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WatchlistFlowTest {

    private WebTestClient client;

    // Only the actual external boundary is mocked - the real
    // InstrumentResolutionService still runs, so it really does resolve,
    // create, and persist Instrument rows via the real InstrumentRepository.
    // WatchlistService/WatchlistRepository/the DB are all real.
    @MockitoBean
    private InstrumentLookupClient instrumentLookupClient;

    @BeforeEach
    void setup(@LocalServerPort int port) {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();

        when(instrumentLookupClient.search("AAPL"))
                .thenReturn(List.of(new InstrumentSearchResult("AAPL", "APPLE INC")));
        when(instrumentLookupClient.search("TSLA"))
                .thenReturn(List.of(new InstrumentSearchResult("TSLA", "TESLA INC")));
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
        Long aaplId = followed.instrumentId();

        WatchlistItemResponse followedTsla = client.post()
                .uri("/watchlist/TSLA")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WatchlistItemResponse.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(followedTsla);
        Long tslaId = followedTsla.instrumentId();

        // following the same instrument twice should be a no-op, not a duplicate/error
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

        assertNotNull(list);
        assertEquals(2, list.instrumentIds().size());
        assertTrue(list.instrumentIds().contains(aaplId));
        assertTrue(list.instrumentIds().contains(tslaId));

        client.delete()
                .uri("/watchlist/{id}", aaplId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();

        // deleting the same instrument twice should be a no-op, not a duplicate/error
        client.delete()
                .uri("/watchlist/{id}", aaplId)
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

        assertNotNull(afterUnfollow);
        assertEquals(1, afterUnfollow.instrumentIds().size());
        assertTrue(afterUnfollow.instrumentIds().contains(tslaId));
    }

    @Test
    void watchlist_requires_auth() {
        client.get().uri("/watchlist").exchange().expectStatus().isUnauthorized();
        client.post().uri("/watchlist/AAPL").exchange().expectStatus().isUnauthorized();
        client.delete().uri("/watchlist/{id}", 1L).exchange().expectStatus().isUnauthorized();
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

    @Test
    void unfollow_unknownInstrumentId_isNoContent_notError() {
        String token = AuthTestUtils.registerAndLogin(client, AuthTestUtils.uniqueUsername(), "1234");

        client.delete()
                .uri("/watchlist/{id}", 999_999L)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();
    }
}