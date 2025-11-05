package com.experis.scheduler.service;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@Slf4j
public class ReportService {

    /**
     * Scrive (o sovrascrive) un report CSV basato su una lista di DTO.
     * @param filePath Path del file di output (da application.yml)
     * @param data Lista di DTO da scrivere
     */
    public <T> void writeReport(String filePath, List<T> data) {
        if (data == null || data.isEmpty()) {
            log.info("Nessun dato da scrivere per il report: {}", filePath);
            return;
        }

        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent()); // Assicura che la cartella esista

            log.info("Inizio scrittura report CSV: {}", filePath);

            try (Writer writer = new FileWriter(filePath, false)) { // false = sovrascrivi
                StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(writer)
                        .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                        .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                        .build();

                beanToCsv.write(data);
                log.info("Report CSV scritto con successo: {} righe in {}", data.size(), filePath);
            }
        } catch (Exception e) {
            log.error("Errore durante la scrittura del report CSV: {}", filePath, e);
        }
    }
}