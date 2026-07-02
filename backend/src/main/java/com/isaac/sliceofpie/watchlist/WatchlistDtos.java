package com.isaac.sliceofpie.watchlist;

import java.util.List;

public class WatchlistDtos {
    public record WatchlistItemResponse(String ticker) {}
    public record WatchlistResponse(List<String> tickers) {}
}