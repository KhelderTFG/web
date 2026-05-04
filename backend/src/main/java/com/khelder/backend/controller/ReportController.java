package com.khelder.backend.controller;

import com.khelder.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * RF-16: Generar y descargar informe
     * GET /api/v1/reports/{patientId}
     *     ?from=2026-01-01T00:00:00
     *     &to=2026-12-31T23:59:59
     *     &format=pdf|csv
     */
    @GetMapping("/{patientId}")
    public ResponseEntity<byte[]> generateReport(
            @PathVariable UUID patientId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            @RequestParam(defaultValue = "pdf") String format
    ) {
        byte[] report = reportService.generateReport(
                patientId, from, to, format
        );

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        String filename  = "informe_khelder_" + timestamp + "." + format;

        MediaType mediaType = format.equalsIgnoreCase("csv")
                ? MediaType.parseMediaType("text/csv")
                : MediaType.APPLICATION_PDF;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(report);
    }
}