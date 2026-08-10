package com.isaac.sliceofpie.instrument;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    Optional<Instrument> findByTicker(String ticker);

    // Used by the scheduled price refresh to pick just the slice of
    // instruments "due" this run, rather than all of them at once - see
    // PriceRefreshScheduler for why. Selecting ids only (not full entities)
    // keeps this cheap and sidesteps handing back entities the caller would
    // otherwise need to re-fetch anyway to safely mutate/persist.
    @Query("SELECT i.id FROM Instrument i WHERE MOD(i.id, :modulus) = :remainder")
    List<Long> findIdsByIdModulo(@Param("modulus") int modulus, @Param("remainder") int remainder);
}