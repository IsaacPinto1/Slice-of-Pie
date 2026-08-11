package com.isaac.sliceofpie.broker;

import com.isaac.sliceofpie.broker.lookup.PositionsClient;
import org.springframework.stereotype.Service;

@Service
public class BrokerConnectionService {

    private final PositionsClient snapTradeClient;

    public BrokerConnectionService(PositionsClient snapTradeClient) {
        this.snapTradeClient = snapTradeClient;
    }

    /**
     * Live call to SnapTrade every time - with a Personal key there's no
     * local BrokerConnection row to check against; "connected" is purely
     * a question SnapTrade itself can answer.
     *
     * TODO(perf): if this turns out to be too slow/frequent on every
     * dashboard load, add a small local status-cache table purely as a
     * performance optimization (NOT anything credential-bearing - no
     * userSecret equivalent exists to cache). Explicitly skipped for v1
     * per product decision.
     */
    public boolean isConnected() {
        return snapTradeClient.hasConnectedAccounts();
    }
}
