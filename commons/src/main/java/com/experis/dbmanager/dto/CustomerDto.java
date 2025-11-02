package com.experis.dbmanager.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CustomerDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int customerId;
    private String username;
    private String password;
    private String email;
    private String customerType;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

}
