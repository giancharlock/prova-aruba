package com.experis.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

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
