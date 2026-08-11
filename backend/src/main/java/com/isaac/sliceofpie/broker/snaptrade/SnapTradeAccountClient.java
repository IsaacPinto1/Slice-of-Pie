package com.isaac.sliceofpie.broker.snaptrade;

import org.springframework.stereotype.Component;

import com.isaac.sliceofpie.broker.BrokerDtos.ClientHoldingResponse;
import com.isaac.sliceofpie.broker.lookup.PositionsClient;
import com.isaac.sliceofpie.broker.snaptrade.SnapTradeDtos.SnapTradeAccount;
import com.isaac.sliceofpie.broker.snaptrade.SnapTradeDtos.SnapTradePosition;

import java.util.List;
import java.util.Map;

@Component
public class SnapTradeAccountClient implements PositionsClient {

    private final SnapTradeSigningClient snapTradeClient;

    public SnapTradeAccountClient(SnapTradeSigningClient snapTradeClient) {
        this.snapTradeClient = snapTradeClient;
    }

    public boolean hasConnectedAccounts() {
        // Should roughly call the below and confirm there's a connected account
        return snapTradeClient.get(
                "/api/v1/accounts",
                Map.of(),
                boolean.class
        );
    }

    public List<ClientHoldingResponse> fetchHoldings() {
        return snapTradeClient.get(
                "/api/v1/accounts/" + accountId,
                Map.of(),
                SnapTradeAccount.class
        );
    }
}