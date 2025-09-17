package com.backend.controller;

import com.backend.service.UpbitApiClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/upbit", produces = MediaType.APPLICATION_JSON_VALUE)
public class UpbitController {

    private final UpbitApiClient upbitApiClient;

    public UpbitController(UpbitApiClient upbitApiClient) {
        this.upbitApiClient = upbitApiClient;
    }

    @GetMapping("/accounts")
    public Mono<String> getAccounts() {
        return upbitApiClient.getAccounts();
    }

    @PostMapping("/orders")
    public Mono<String> placeOrder(
            @RequestParam String market,
            @RequestParam String side,
            @RequestParam(required = false) String volume,
            @RequestParam(required = false) String price,
            @RequestParam("ord_type") String ordType
    ) {
        return upbitApiClient.placeOrder(market, side, volume, price, ordType);
    }
}


