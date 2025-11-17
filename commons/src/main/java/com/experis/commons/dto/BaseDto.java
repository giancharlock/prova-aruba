package com.experis.commons.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaseDto {
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
