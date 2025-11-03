package com.experis.scheduler.service;

import com.experis.scheduler.config.SchedulerProperties;
import com.experis.scheduler.dto.InvoiceReportDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class DbManagerClient {

    private final WebClient webClient;
    private final String dbmanagerApiUrl;

    public DbManagerClient(WebClient.Builder webClientBuilder, SchedulerProperties properties) {
        this.webClient = webClientBuilder.baseUrl(properties.getDbmanagerUrl()).build();
        this.dbmanagerApiUrl = properties.getDbmanagerUrl();
    }

    /**
     * Recupera tutte le fatture da dbmanager per il report.
     * Assumiamo che dbmanager/api/invoices ritorni una lista (o pagina) di InvoiceDto.
     */
    public Mono<List<InvoiceReportDto>> fetchAllInvoices() {
        log.info("Interrogo dbmanager per il report fatture: {}", dbmanagerApiUrl + "/invoices");

        // Definiamo un tipo di riferimento per la risposta paginata (se usata da dbmanager)
        // Se dbmanager non usa la paginazione, cambiare in List<InvoiceReportDto>
        ParameterizedTypeReference<RestResponsePage<InvoiceReportDto>> pageTypeRef =
                new ParameterizedTypeReference<>() {};

        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/invoices")
                        .queryParam("size", 1000) // Chiediamo una pagina grande
                        .build())
                .retrieve()
                .bodyToMono(pageTypeRef)
                .map(page -> {
                    log.info("Ricevute {} fatture da dbmanager", page.getTotalElements());
                    // Appiattiamo i dati del customer nel DTO
                    page.getContent().forEach(InvoiceReportDto::flattenCustomerData);
                    return page.getContent();
                })
                .doOnError(e -> log.error("Errore durante la chiamata a dbmanager", e))
                .onErrorResume(e -> Mono.empty()); // Continua anche se dbmanager non risponde
    }

    // Classe di supporto per deserializzare la Page<> di Spring Data Rest
    // (Spesso necessaria quando si chiama un altro microservizio Spring)
    static class RestResponsePage<T> implements Page<T> {
        // ... implementazione completa di Page omessa per brevità ...
        // I campi importanti sono 'content' e 'totalElements'
        private List<T> content;
        private long totalElements;

        public List<T> getContent() { return content; }
        public long getTotalElements() { return totalElements; }

        // Metodi dell'interfaccia Page...
    }
}