package com.experis.dbmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerDto extends BaseDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int customerId;
    private String username;
    private String password;
    private String email;
    private String customerType;

}
