package com.experis.scheduler.service;

import com.experis.scheduler.config.SchedulerProperties;
import com.experis.scheduler.dto.InvoiceReportDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
    static class RestResponsePage<T> extends PageImpl<T> {

        /**
         * Costruttore utilizzato da Jackson per deserializzare la risposta JSON.
         * @param content La lista di elementi nella pagina.
         * @param number Il numero della pagina corrente.
         * @param size La dimensione della pagina.
         * @param totalElements Il numero totale di elementi disponibili.
         * @param pageable Oggetto JSON che descrive la paginazione (ignorato, ricostruito da number/size).
         * @param last Flag che indica se questa è l'ultima pagina.
         * @param totalPages Il numero totale di pagine.
         * @param sort Oggetto JSON che descrive l'ordinamento (ignorato).
         * @param first Flag che indica se questa è la prima pagina.
         * @param numberOfElements Il numero di elementi in questa pagina specifica.
         */
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public RestResponsePage(@JsonProperty("content") List<T> content,
                                @JsonProperty("number") int number,
                                @JsonProperty("size") int size,
                                @JsonProperty("totalElements") Long totalElements,
                                @JsonProperty("pageable") JsonNode pageable,
                                @JsonProperty("last") boolean last,
                                @JsonProperty("totalPages") int totalPages,
                                @JsonProperty("sort") JsonNode sort,
                                @JsonProperty("first") boolean first,
                                @JsonProperty("numberOfElements") int numberOfElements) {

            // Chiama il costruttore della classe base PageImpl.
            // Le informazioni essenziali sono il contenuto, un oggetto Pageable (che creiamo al volo)
            // e il numero totale di elementi. PageImpl si occuperà di calcolare il resto.
            super(content, PageRequest.of(number, size), totalElements);
        }

        // Costruttori standard di PageImpl, utili per i test o altre istanziazioni.
        public RestResponsePage(List<T> content, Pageable pageable, long total) {
            super(content, pageable, total);
        }

        public RestResponsePage(List<T> content) {
            super(content);
        }

        public RestResponsePage() {
            super(new ArrayList<>());
        }
    }
}