package com.telehealth.anonymization.repository;

import com.telehealth.anonymization.model.TokenNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TokenNodeRepository extends Neo4jRepository<TokenNode, String> {

    @Query("MATCH (t:Token {tokenValue: $tokenValue}) DETACH DELETE t")
    void detachAndDeleteToken(@Param("tokenValue") String tokenValue);

    @Query("MATCH (t:Token) WHERE t.expiresAt < $now DETACH DELETE t")
    void deleteAllExpiredTokens(@Param("now") LocalDateTime now);
}
