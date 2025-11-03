package com.experis.receiver.service;

import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.dto.ResponseDto;
import com.experis.dbmanager.dto.SdiNotificationDto;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.CompletableFuture;

public interface IReceiverService {

    /**
     * Gestisce la ricezione di una fattura interna.
     * La imposta come INTERNAL_INVOICE_NEW e la invia a Kafka.
     * Restituisce una risposta immediata (202 Accepted) e gestisce la callback asincrona.
     *
     * @param invoice - InvoiceDto Object
     * @return un CompletableFuture che si risolverà con la risposta finale.
     */
    CompletableFuture<ResponseEntity<ResponseDto>> saveInternalInvoice(InvoiceDto invoice);

    /**
     * Gestisce la ricezione di una fattura esterna.
     * La imposta come EXTERNAL_INVOICE e la invia a Kafka.
     * Restituisce una risposta immediata (202 Accepted) e gestisce la callback asincrona.
     *
     * @param invoice - InvoiceDto Object
     * @return un CompletableFuture che si risolverà con la risposta finale.
     */
    CompletableFuture<ResponseEntity<ResponseDto>> saveExternalInvoice(InvoiceDto invoice);

    /**
     * Riceve una notifica da SdI (tramite endpoint HTTP).
     * La inoltra al topic Kafka DSI_NOTIFICATION.
     *
     * @param notification - SdiNotificationDto Object
     * @return un CompletableFuture che si risolverà con la risposta finale.
     */
    CompletableFuture<ResponseEntity<ResponseDto>> handleSdiNotification(SdiNotificationDto notification);

}