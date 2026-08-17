package com.isaac.sliceofpie.broker.snaptrade;

import org.springframework.stereotype.Component;

import com.isaac.sliceofpie.broker.PositionDtos.BrokerHolding;
import com.isaac.sliceofpie.broker.lookup.BrokerClient;
import com.isaac.sliceofpie.broker.snaptrade.SnapTradeDtos.SnapTradeAccount;
import com.isaac.sliceofpie.broker.snaptrade.SnapTradeDtos.SnapTradeInstrument;
import com.isaac.sliceofpie.broker.snaptrade.SnapTradeDtos.SnapTradePosition;
import com.isaac.sliceofpie.broker.snaptrade.SnapTradeDtos.SnapTradePositionsResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SnapTrade-backed implementation of BrokerClient. This package is the
 * only place in the codebase that should know SnapTrade's API shape -
 * everything outside it talks to BrokerClient / BrokerHolding. Swapping
 * providers means writing a new implementation of BrokerClient and
 * rewiring the bean; nothing else should need to change.
 */
@Component
public class SnapTradeAccountClient implements BrokerClient {

    private final SnapTradeSigningClient snapTradeClient;

    public SnapTradeAccountClient(SnapTradeSigningClient snapTradeClient) {
        this.snapTradeClient = snapTradeClient;
    }

    @Override
    public boolean hasConnectedAccounts() {
        SnapTradeAccount[] accounts = listAccounts();
        return accounts.length > 0;
    }

    /**
     * Aggregates positions across every account connected under this
     * Personal key. A Position is keyed one-per-(user, instrument), not
     * one-per-(user, account, instrument), so the same ticker held in more
     * than one account is summed into a single BrokerHolding here rather
     * than left for PositionService to reconcile.
     */
    @Override
    public List<BrokerHolding> fetchHoldings() {
        SnapTradeAccount[] accounts = listAccounts();
        if (accounts.length == 0) {
            return List.of();
        }

        // ticker -> holding, so quantities from the same ticker in
        // different accounts add up instead of one overwriting another.
        Map<String, BrokerHolding> holdingsByTicker = new LinkedHashMap<>();

        for (SnapTradeAccount account : accounts) {
            SnapTradePositionsResponse response = snapTradeClient.get(
                    "/accounts/" + account.id() + "/positions/all",
                    Map.of(),
                    SnapTradePositionsResponse.class);

            if (response == null || response.results() == null) {
                continue;
            }

            for (SnapTradePosition position : response.results()) {
                mergeHolding(holdingsByTicker, position);
            }
        }

        return List.copyOf(holdingsByTicker.values());
    }

    private SnapTradeAccount[] listAccounts() {
        SnapTradeAccount[] accounts =
                snapTradeClient.get("/accounts", Map.of(), SnapTradeAccount[].class);
        return accounts == null ? new SnapTradeAccount[0] : accounts;
    }

    private void mergeHolding(Map<String, BrokerHolding> holdingsByTicker, SnapTradePosition position) {
        SnapTradeInstrument instrument = position.instrument();
        if (instrument == null || instrument.symbol() == null || position.units() == null) {
            // Cash-equivalent or otherwise non-instrument positions don't
            // map to a Position row - skip rather than fail the whole sync.
            return;
        }

        String ticker = instrument.symbol();
        BigDecimal quantity = position.units();
        // SnapTrade doesn't guarantee cost_basis on every position shape
        // (e.g. it's routinely absent for cash-like or newly-opened
        // positions) - default to zero rather than let a null ripple into
        // the merge math below or the non-nullable Position column.
        BigDecimal costBasis = position.cost_basis() != null ? position.cost_basis() : BigDecimal.ZERO;
        String name = instrument.description();

        holdingsByTicker.merge(
            ticker,
            new BrokerHolding(ticker, name, quantity, costBasis),
            (existing, incoming) -> new BrokerHolding(
                    ticker,
                    existing.name() != null ? existing.name() : incoming.name(),
                    existing.quantity().add(incoming.quantity()),
                    calculateCostBasis(
                        existing.costBasis(),
                        existing.quantity(),
                        incoming.costBasis(),
                        incoming.quantity())
                    )
        );
    }

    private BigDecimal calculateCostBasis(BigDecimal p1, BigDecimal q1, BigDecimal p2, BigDecimal q2) {
        BigDecimal totalQuantity = q1.add(q2);

        if (totalQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return p1.multiply(q1)
                .add(p2.multiply(q2))
                .divide(totalQuantity, 8, RoundingMode.HALF_UP);
    }
}
