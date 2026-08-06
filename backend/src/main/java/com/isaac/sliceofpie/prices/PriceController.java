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

    @GetMapping
    public PriceResponse getPrice(@RequestParam("ticker") String ticker){
        return priceService.getPrice(ticker);
    }
}
