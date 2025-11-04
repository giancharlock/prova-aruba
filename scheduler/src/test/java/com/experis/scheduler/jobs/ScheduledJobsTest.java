package com.experis.scheduler.jobs;

import com.experis.scheduler.dto.DltMessageDto;
import com.experis.scheduler.dto.InvoiceReportDto;
import com.experis.scheduler.service.DbManagerClient;
import com.experis.scheduler.service.ReportService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test per i job schedulati.
 *
 * @SpringBootTest: Carica l'intero contesto Spring.
 * @ActiveProfiles("test"): Attiva il profilo "test" (usa application-test.yml).
 * @EmbeddedKafka: Avvia un broker Kafka finto sulla porta specificata in application-test.yml.
 * @DirtiesContext: Ricrea il contesto Spring dopo ogni test (utile per Kafka).
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EmbeddedKafka(partitions = 1, topics = { "test-dlt-job.DLT" })
class ScheduledJobsTest {

    // Vengono iniettati i bean reali
    @Autowired
    private ScheduledJobs scheduledJobs;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate; // Per inviare messaggi di test

    // Questi bean vengono "mockati" (sostituiti con versioni finte)
    // per isolare il test alla sola logica del Job.
    @MockBean
    private ReportService reportService;

    @MockBean
    private DbManagerClient dbManagerClient;

    private String dltTopic = "test-dlt-job.DLT";

    @BeforeEach
    void setUp() {
        // Pulisce eventuali chiamate precedenti ai mock
        reset(reportService, dbManagerClient);
    }

    /**
     * Test per il Job 1: Lettura DLT e Report
     */
    @Test
    void testReadDltAndReportJob() throws InterruptedException {
        // --- ARRANGE ---
        // 1. Prepariamo un messaggio DLT finto
        String payload = "{\"error\": \"messaggio illeggibile\"}";
        String key = "invoice-123";

        ProducerRecord<String, String> record = new ProducerRecord<>(dltTopic, key, payload);
        // Aggiungiamo un header finto
        record.headers().add(new RecordHeader("kafka_dlt-exception-message", "Errore di deserializzazione".getBytes(StandardCharsets.UTF_8)));

        // 2. Inviamo il messaggio al Kafka "embedded"
        kafkaTemplate.send(record).join(); // .join() attende l'invio

        // 3. Prepariamo il "captor" per catturare l'input del ReportService
        ArgumentCaptor<List<DltMessageDto>> dltCaptor = ArgumentCaptor.forClass(List.class);

        // --- ACT ---
        // Eseguiamo manualmente il job
        scheduledJobs.readDltAndReport();

        // --- ASSERT ---
        // 1. Verifichiamo che reportService.writeReport() sia stato chiamato 1 volta
        // con il path corretto e il tipo di classe corretto.
        verify(reportService, times(1)).writeReport(
                eq("target/test-reports/dlt_report.csv"),
                dltCaptor.capture(),
                eq(DltMessageDto.class)
        );

        // 2. Analizziamo cosa è stato passato al reportService
        List<DltMessageDto> capturedList = dltCaptor.getValue();
        assertThat(capturedList).isNotNull();
        assertThat(capturedList).hasSize(1); // Deve aver trovato 1 messaggio DLT

        // 3. Verifichiamo il contenuto del messaggio catturato
        DltMessageDto capturedDto = capturedList.get(0);
        assertThat(capturedDto.getKey()).isEqualTo(key);
        assertThat(capturedDto.getTopic()).isEqualTo(dltTopic);
        assertThat(capturedDto.getPayloadPreview()).contains("messaggio illeggibile");
        assertThat(capturedDto.getHeaders()).contains("Errore di deserializzazione");
    }

    /**
     * Test per il Job 2: Report Fatture da DBManager
     */
    @Test
    void testGenerateInvoiceStatusReportJob() {
        // --- ARRANGE ---
        // 1. Prepariamo una risposta finta dal DbManagerClient
        InvoiceReportDto invoice1 = new InvoiceReportDto();
        invoice1.setInvoiceNumber(101);
        invoice1.setInvoiceStatus("INTERNAL_INVOICE_SENT");
        invoice1.setCustomerUsername("utente_1");
        invoice1.setCustomerEmail("utente1@mail.com");
        invoice1.setCreatedAt(LocalDateTime.now().minusDays(1));
        invoice1.setCreatedBy("DBMANAGER_MS");

        List<InvoiceReportDto> mockInvoiceList = List.of(invoice1);

        // 2. Configuriamo il mock: "Quando dbManagerClient.fetchAllInvoices() viene chiamato,
        //    restituisci la nostra lista finta."
        when(dbManagerClient.fetchAllInvoices()).thenReturn(Mono.just(mockInvoiceList));

        // 3. Prepariamo il "captor"
        ArgumentCaptor<List<InvoiceReportDto>> invoiceCaptor = ArgumentCaptor.forClass(List.class);

        // --- ACT ---
        // Eseguiamo manualmente il job
        scheduledJobs.generateInvoiceStatusReport();

        // --- ASSERT ---
        // 1. Verifichiamo che dbManagerClient.fetchAllInvoices() sia stato chiamato
        verify(dbManagerClient, times(1)).fetchAllInvoices();

        // 2. Verifichiamo che reportService.writeReport() sia stato chiamato 1 volta
        verify(reportService, times(1)).writeReport(
                eq("target/test-reports/invoice_status_report.csv"),
                invoiceCaptor.capture(),
                eq(InvoiceReportDto.class)
        );

        // 3. Verifichiamo che la lista passata al report sia quella finta
        List<InvoiceReportDto> capturedList = invoiceCaptor.getValue();
        assertThat(capturedList).isNotNull();
        assertThat(capturedList).hasSize(1);
        assertThat(capturedList.get(0).getInvoiceNumber()).isEqualTo(101);
        assertThat(capturedList.get(0).getCustomerUsername()).isEqualTo("utente_1");
    }

    /**
     * Test per il Job 2 quando DBManager non restituisce fatture
     */
    @Test
    void testGenerateInvoiceStatusReportJob_WhenNoInvoices() {
        // --- ARRANGE ---
        // 1. Configuriamo il mock per restituire una lista vuota
        when(dbManagerClient.fetchAllInvoices()).thenReturn(Mono.just(List.of()));

        // --- ACT ---
        scheduledJobs.generateInvoiceStatusReport();

        // --- ASSERT ---
        // 1. Verifichiamo che dbManagerClient sia stato chiamato
        verify(dbManagerClient, times(1)).fetchAllInvoices();

        // 2. Verifichiamo che reportService.writeReport() NON sia stato chiamato
        verify(reportService, never()).writeReport(any(), any(), any());
    }
}