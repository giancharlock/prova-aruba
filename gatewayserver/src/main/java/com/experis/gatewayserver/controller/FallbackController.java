package com.experis.gatewayserver.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    /**
     * Quando il Gateway tenta di inoltrare una richiesta a un microservizio,
     * ma quel servizio è irraggiungibile, lento o restituisce un errore,
     * il FallbackController fornisce una risposta predefinita all'utente finale,
     * invece di un errore 503 Service Unavailable o un timeout.
     * @return
     */
    @RequestMapping("/contactSupport")
    public Mono<String> contactSupport() {
        return Mono.just("An error occurred. Please try after some time or contact support team!!!");
    }

}
