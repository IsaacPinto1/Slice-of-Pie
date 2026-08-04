# Project brief: personal portfolio + thesis tracker (v1)

I'm building a personal, single-user web app. Please use this as full context for helping me implement it — I'm picking this up in a new session after planning it out elsewhere.

## What it does (v1 scope only)

1. Shows my real Robinhood stock positions (read-only).
2. Lets me write/edit my own free-text "thesis" notes on positions I hold.
3. Lets me research and write thesis notes on stocks I *don't* own yet (a watchlist).

**Explicitly out of scope for this phase: no trading, no order placement, no brokerage write access of any kind.** Don't suggest or implement SnapTrade trading endpoints, Robinhood's Agentic Trading, Alpaca, Tradier, or any execution path. Those were discussed as a possible *future*, separate phase requiring a different brokerage/account setup — not part of this build.

## Tech stack

- **Backend:** Java, Spring Boot
- **Frontend:** not opinionated — pick whatever's fastest to stand up (plain React or even server-rendered HTML+JS is fine)
- **Database:** PostgreSQL, with Flyway for schema migrations
- **Brokerage data:** SnapTrade (not Plaid)
- **Price data:** Finnhub free tier

## Why SnapTrade, not Plaid

- SnapTrade's free plan is a permanent personal-use tier: read-only, real-time portfolio data, up to 5 brokerage connections for one user — not a time-limited trial.
- Its data model (positions, quantity, cost basis, normalized tickers) maps directly onto "show me what I own."
- Plaid's free option is a newer "Trial" plan (10 items, only for teams created after April 2026), and Plaid's core product is bank accounts/transactions — workable, but more general-purpose than this needs.
- Robinhood connects to SnapTrade via OAuth (read-only); Robinhood has no developer API of its own.

## SnapTrade integration notes

- Flow: register a SnapTrade user server-side (get back `userId` + `userSecret` — store the secret encrypted, never log it) → generate a Connection Portal URL → user completes the hosted flow (accept SnapTrade's terms once, get redirected to Robinhood, log in, accept Robinhood's Access User Agreement) → redirected back to the app → call List Accounts → call Account Positions whenever needed.
- Re-authentication is **event-driven, not scheduled** — a connection mainly breaks if I change my Robinhood password or explicitly revoke access, not on a fixed timer.
- Subscribe to the `CONNECTION_BROKEN` webhook so the backend can proactively show "reconnect needed" instead of discovering it from failed API calls. (Note: this requires the backend to be reachable from the internet, not just localhost, for the webhook to land.)
- Trading is not available for Robinhood through SnapTrade at all — irrelevant here since trading is out of scope, but don't waste time looking for it.

## Price data approach

- Finnhub free tier: 60 calls/minute, ~15-20 min delayed quotes — more than enough for "within a minute or so" freshness on a small personal watchlist.
- The frontend should **never** call Finnhub directly. Use a single backend-side scheduled job (Spring `@Scheduled`) that refreshes prices into Postgres on a fixed interval (e.g. every 60 seconds), only during market hours (9:30am–4pm ET, weekdays). API usage should stay flat no matter how many browser tabs are open.
- Only poll symbols actually in use: held positions + watchlist tickers.
- Compute "% of portfolio" myself from (cached price × share count from SnapTrade) rather than trusting SnapTrade's own market-value field — share counts barely change, but the vendor's valuation may only sync with Robinhood about once a day.

## Architecture (data flow)

```
Robinhood --(SnapTrade, read-only)--> Scheduled sync job --> Postgres (positions, theses, watchlist)
Finnhub   --(price polling)--------->        ^                        |
                                              |                        v
                                              +------------------ REST API <--> Web/mobile client
```

## Suggested file structure (monorepo)

```
portfolio-thesis-app/
├── backend/
│   ├── src/main/java/com/<you>/portfolioapp/
│   │   ├── PortfolioAppApplication.java
│   │   ├── config/          # scheduler config, CORS, secrets loading
│   │   ├── snaptrade/       # SnapTrade client wrapper, connection flow, DTOs
│   │   ├── prices/          # Finnhub client, price cache entity/repo
│   │   ├── sync/            # scheduled jobs tying snaptrade + prices together
│   │   ├── positions/       # Position entity, repository, service, controller
│   │   ├── thesis/          # Thesis entity, repository, service, controller
│   │   ├── watchlist/       # Watchlist entity, repository, service, controller
│   │   └── common/          # shared exceptions/utils
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/    # Flyway SQL migrations
│   ├── src/test/java/...
│   └── pom.xml
├── frontend/
│   ├── src/
│   └── package.json
├── docs/
├── .gitignore
└── README.md
```

Organized by feature (`snaptrade/`, `positions/`, `thesis/`, `watchlist/`), not by layer — keeps everything about one concept together. `sync/` is separate because it's the only package that depends on both `snaptrade/` and `prices/`.

## Ground rules for this build

- Keep thesis notes as plain text + timestamp tied to a ticker for v1. Don't add tagging, sentiment scoring, or price-target tracking yet — that's the most likely place scope quietly expands.
- Single user, no auth/multi-tenancy needed beyond whatever's required to keep my SnapTrade `userSecret` and Finnhub key secure.
- Free-tier limits I'm designing around: SnapTrade's 5-connection free cap (I only need 1) and Finnhub's 60 calls/minute (plenty for a personal watchlist).

## Where I'd like help next

Help me start implementing this — happy to begin wherever makes sense (Spring Boot project scaffolding, the SnapTrade connection flow, the database schema, or the sync job) but I'd suggest starting with the project skeleton and database schema first.
