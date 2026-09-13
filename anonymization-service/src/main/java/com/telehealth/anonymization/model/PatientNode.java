package com.telehealth.anonymization.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Node("Patient")
@Data
@Builder
public class PatientNode {

    @Id
    private UUID patientId;

    @Relationship(type = "ISSUED", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<TokenNode> tokens = new HashSet<>();
}
