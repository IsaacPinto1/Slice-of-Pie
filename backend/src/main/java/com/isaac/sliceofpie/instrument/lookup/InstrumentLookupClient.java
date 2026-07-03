package com.isaac.sliceofpie.instrument.lookup;

import java.util.List;

import com.isaac.sliceofpie.instrument.InstrumentDtos.InstrumentSearchResult;

/**
 * Contract for resolving a free-text query (ticker or company name) into
 * candidate instruments. Implementations are provider-specific (Finnhub,
 * etc.) and live in their own subpackage - nothing outside this package
 * and its implementations should know or care which provider is behind it.
 *
 * To switch providers: write a new implementation of this interface and
 * change which one is wired as the Spring bean. Nothing in the domain
 * layer (InstrumentResolutionService, etc.) should need to change.
 */
public interface InstrumentLookupClient {

    List<InstrumentSearchResult> search(String query);
}