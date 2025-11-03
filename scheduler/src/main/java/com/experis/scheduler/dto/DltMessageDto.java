package com.experis.scheduler.dto;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DltMessageDto {

    @CsvBindByName(column = "Timestamp")
    @CsvDate("yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @CsvBindByName(column = "Topic_DLT")
    private String topic;

    @CsvBindByName(column = "Partition")
    private int partition;

    @CsvBindByName(column = "Offset")
    private long offset;

    @CsvBindByName(column = "Key")
    private String key;

    @CsvBindByName(column = "Headers")
    private String headers;

    @CsvBindByName(column = "Payload_Preview")
    private String payloadPreview;
}