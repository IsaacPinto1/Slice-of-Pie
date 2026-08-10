package com.isaac.sliceofpie.prices;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.isaac.sliceofpie.prices.PriceDtos.PriceResponse;

@RestController
@RequestMapping("/price")
public class PriceController {
    
    private final PriceService priceService;

    public PriceController(PriceService priceService){
        this.priceService = priceService;
    }

    /* No ticker validation occurs here because this should only be called
    from existing instruments, which have validated tickers */
    @GetMapping
    public PriceResponse getPrice(@RequestParam("instrumentId") Long instrumentId){
        return priceService.getPrice(instrumentId);
    }

    @GetMapping("/force")
    public PriceResponse forceLatestPrice(@RequestParam("instrumentId") Long instrumentId){
        return priceService.forceLatestPrice(instrumentId);
    }
}
