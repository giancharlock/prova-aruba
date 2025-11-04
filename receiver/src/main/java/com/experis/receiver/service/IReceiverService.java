package com.experis.receiver.service;

import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.dto.ResponseDto;
import com.experis.dbmanager.dto.SdiNotificationDto;
import org.springframework.http.ResponseEntity;

public interface IReceiverService {

    /**
     * Gestisce la ricezione di una fattura interna.
     * La imposta come INTERNAL_INVOICE_NEW e la invia a Kafka.
     * Restituisce una risposta immediata (es. 202 Accepted).
     *
     * @param invoice - InvoiceDto Object
     * @return una ResponseEntity immediata.
     */
    ResponseEntity<ResponseDto> saveInternalInvoice(InvoiceDto invoice);

    /**
     * Gestisce la ricezione di una fattura esterna.
     * La imposta come EXTERNAL_INVOICE e la invia a Kafka.
     * Restituisce una risposta immediata (es. 202 Accepted).
     *
     * @param invoice - InvoiceDto Object
     * @return una ResponseEntity immediata.
     */
    ResponseEntity<ResponseDto> saveExternalInvoice(InvoiceDto invoice);

    /**
     * Riceve una notifica da SdI (tramite endpoint HTTP).
     * La inoltra al topic Kafka dsiNotification.
     *
     * @param notification - SdiNotificationDto Object
     * @return una ResponseEntity immediata.
     */
    ResponseEntity<ResponseDto> handleSdiNotification(SdiNotificationDto notification);

    void handleSavedInvoice(InvoiceDto savedInvoice);
    void handleUpdatedInvoice(SdiNotificationDto sdiNotificationDto);
}