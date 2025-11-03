package com.experis.scheduler.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InvoiceReportDto {

    // Dati richiesti dal README.md

    @CsvBindByName(column = "Stato_Fattura")
    private String invoiceStatus;

    @CsvBindByName(column = "Numero_Fattura")
    private int invoiceNumber;

    // Assumiamo che il customer sia un oggetto nestato nel DTO di risposta da dbmanager
    @CsvBindByName(column = "Username_Customer")
    private String customerUsername;

    @CsvBindByName(column = "Email_Customer")
    private String customerEmail;

    @CsvBindByName(column = "Data_Creazione")
    private LocalDateTime createdAt;

    @CsvBindByName(column = "Creato_Da")
    private String createdBy;

    @CsvBindByName(column = "Data_Update")
    private LocalDateTime updatedAt;

    @CsvBindByName(column = "Aggiornato_Da")
    private String updatedBy;

    // DTO per mappare la risposta (potenzialmente paginata) da dbmanager
    // Questi campi sono necessari per mappare la risposta completa
    private CustomerDto customer;

    // Campi per mappare Customer (classe interna o separata)
    @Data
    public static class CustomerDto {
        private String username;
        private String email;
    }

    // Metodo helper per appiattire i dati prima di scriverli
    public void flattenCustomerData() {
        if (customer != null) {
            this.customerUsername = customer.getUsername();
            this.customerEmail = customer.getEmail();
        }
    }
}