package com.awl.hackathontesttaskbackend.request;

import lombok.Data;

@Data
public class EmailMessageRequest {
    private String subject;
    private String message;
}
