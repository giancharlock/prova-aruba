package com.experis.scheduler.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app.scheduler")
@Validated
@Data
public class SchedulerProperties {

    @NotEmpty
    private String dbmanagerUrl;

    @NotEmpty
    private String dltReportPath;

    @NotEmpty
    private String invoiceReportPath;

    @Valid
    @NotNull
    private DltJob dltJob;

    @Valid
    @NotNull
    private InvoiceReportJob invoiceReportJob;

    @Data
    public static class DltJob {
        @NotEmpty
        private String cron;
        @NotEmpty
        private List<String> topics;
        private Duration pollTimeout = Duration.ofMinutes(1);
    }

    @Data
    public static class InvoiceReportJob {
        @NotEmpty
        private String cron;
    }
}