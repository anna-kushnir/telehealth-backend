package com.telehealth.anonymization.repository;

import com.telehealth.anonymization.model.PatientNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientNodeRepository extends Neo4jRepository<PatientNode, UUID> {

    @Query("MATCH (p:Patient)-[:ISSUED]->(t:Token {tokenValue: $tokenValue}) " +
            "WHERE t.expiresAt > $now " +
            "RETURN p.patientId")
    Optional<UUID> findPatientIdByValidToken(@Param("tokenValue") String tokenValue,
                                             @Param("now") LocalDateTime now);
}
