package com.telehealth.anonymization.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDateTime;

@Node("Token")
@Data
@Builder
public class TokenNode {

    @Id
    private String tokenValue;

    private LocalDateTime expiresAt;
}
