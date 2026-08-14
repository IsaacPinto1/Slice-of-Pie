package com.isaac.sliceofpie.instrument;

/**
 * Published once a brand-new Instrument row has been persisted (see
 * InstrumentResolutionService.createInstrument()). PriceService listens for
 * this - after the creating transaction commits - to fetch an initial price
 * immediately instead of leaving the instrument at price=0 until
 * PriceRefreshScheduler's next pass.
 *
 * Deliberately just the id: by the time any listener runs (after commit),
 * the Instrument itself may already be stale to hold a reference to.
 */
public record InstrumentCreatedEvent(Long instrumentId) {
}
