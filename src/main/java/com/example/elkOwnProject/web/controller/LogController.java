package com.example.elkOwnProject.web.controller;

import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class LogController {
    private final ElasticsearchOperations elasticsearchOperations;

    public LogController(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @PostMapping("/log")
    public ResponseEntity<String> saveLog(@RequestBody String message) {
        try {
            LogMessage logMessage = new LogMessage(message);
            IndexQuery indexQuery = new IndexQueryBuilder()
                    .withObject(logMessage)
                    .build();
            IndexCoordinates indexCoordinates = IndexCoordinates.of("my-logs"); // specify the index name
            String documentId = elasticsearchOperations.index(indexQuery, indexCoordinates);
            elasticsearchOperations.indexOps(IndexCoordinates.of("my-logs")).refresh();
            return ResponseEntity.ok().body("Log saved with id: " + documentId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving log: " + e.getMessage());
        }

    }

    @GetMapping("/log")
    public ResponseEntity<List<String>> getAllLogs() {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchAllQuery())
                .withPageable(PageRequest.of(0, 100));

        SearchHits<LogMessage> searchHits = elasticsearchOperations.search(queryBuilder.build(), LogMessage.class, IndexCoordinates.of("my-logs"));
        Iterable<LogMessage> logs = searchHits.map(SearchHit::getContent);

        List<String> logMessages = new ArrayList<>();
        for (LogMessage log : logs) {
            logMessages.add(log.getMessage());
        }

        return ResponseEntity.ok(logMessages);
    }
}