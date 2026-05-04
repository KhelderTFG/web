package com.khelder.backend.service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.khelder.backend.entity.BiometricRecord;
import com.khelder.backend.entity.MedicalHistory;
import com.khelder.backend.entity.Patient;
import com.khelder.backend.repository.BiometricRecordRepository;
import com.khelder.backend.repository.CaregiverRepository;
import com.khelder.backend.repository.MedicalHistoryRepository;
import com.khelder.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final BiometricRecordRepository biometricRecordRepository;
    private final PatientRepository         patientRepository;
    private final MedicalHistoryRepository  medicalHistoryRepository;
    private final CaregiverRepository       caregiverRepository;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // -------------------------------------------------------------------------
    // RF-16: Generar informe en el formato solicitado
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public byte[] generateReport(
            UUID          patientId,
            LocalDateTime from,
            LocalDateTime to,
            String        format
    ) {
        UUID caregiverId = getAuthenticatedCaregiverId();

        if (!patientRepository.existsAssignment(caregiverId, patientId)) {
            throw new SecurityException(
                "No tienes permisos para generar informes de este paciente"
            );
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Paciente no encontrado: " + patientId
                ));

        MedicalHistory history = medicalHistoryRepository
                .findByPatientPatientId(patientId)
                .orElse(null);

        List<BiometricRecord> records =
                biometricRecordRepository
                        .findAllByPatientIdAndTimestampBetween(
                                patientId, from, to
                        );

        log.info("Generando informe {} para paciente {} con {} registros",
                format, patientId, records.size());

        return switch (format.toLowerCase()) {
            case "csv" -> generateCsv(patient, records);
            default    -> generatePdf(patient, history, records, from, to);
        };
    }

    // -------------------------------------------------------------------------
    // RF-16: Generación de PDF
    // -------------------------------------------------------------------------

    private byte[] generatePdf(
            Patient             patient,
            MedicalHistory      history,
            List<BiometricRecord> records,
            LocalDateTime       from,
            LocalDateTime       to
    ) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            // ---- Cabecera ----
            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 18, new BaseColor(26, 140, 122)
            );
            Font headerFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 12
            );
            Font normalFont = FontFactory.getFont(
                    FontFactory.HELVETICA, 10
            );

            Paragraph title = new Paragraph(
                    "Informe Biométrico — Khelder", titleFont
            );
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // ---- Período del informe ----
            Paragraph period = new Paragraph(
                    "Período: " + from.format(FORMATTER) +
                    " — " + to.format(FORMATTER),
                    normalFont
            );
            period.setAlignment(Element.ALIGN_CENTER);
            period.setSpacingAfter(20);
            document.add(period);

            // ---- Datos del paciente ----
            document.add(new Paragraph("Datos del Paciente", headerFont));
            document.add(new Paragraph(
                    "Nombre: " + patient.getFullName(), normalFont
            ));
            document.add(new Paragraph(
                    "Fecha de nacimiento: " + patient.getDateOfBirth(), normalFont
            ));

            if (history != null) {
                document.add(new Paragraph(
                        "Grupo sanguíneo: " +
                        (history.getBloodType() != null ? history.getBloodType() : "—"),
                        normalFont
                ));
                document.add(new Paragraph(
                        "Alergias: " +
                        (history.getAllergies() != null ? history.getAllergies() : "—"),
                        normalFont
                ));
                document.add(new Paragraph(
                        "Condiciones crónicas: " +
                        (history.getChronicConditions() != null
                                ? history.getChronicConditions() : "—"),
                        normalFont
                ));
            }

            // ---- Resumen estadístico ----
            document.add(new Paragraph("\nResumen del período", headerFont));

            if (!records.isEmpty()) {
                double avgHr = records.stream()
                        .filter(r -> r.getHeartRate() != null)
                        .mapToDouble(BiometricRecord::getHeartRate)
                        .average().orElse(0);

                double maxHr = records.stream()
                        .filter(r -> r.getHeartRate() != null)
                        .mapToDouble(BiometricRecord::getHeartRate)
                        .max().orElse(0);

                double minHr = records.stream()
                        .filter(r -> r.getHeartRate() != null)
                        .mapToDouble(BiometricRecord::getHeartRate)
                        .min().orElse(0);

                long totalSteps = records.stream()
                        .filter(r -> r.getSteps() != null)
                        .mapToLong(BiometricRecord::getSteps)
                        .sum();

                document.add(new Paragraph(
                        "Total de registros: " + records.size(), normalFont
                ));
                document.add(new Paragraph(
                        String.format("FC media: %.1f bpm", avgHr), normalFont
                ));
                document.add(new Paragraph(
                        String.format("FC máxima: %.1f bpm", maxHr), normalFont
                ));
                document.add(new Paragraph(
                        String.format("FC mínima: %.1f bpm", minHr), normalFont
                ));
                document.add(new Paragraph(
                        "Pasos totales: " + totalSteps, normalFont
                ));
            } else {
                document.add(new Paragraph(
                        "No hay registros en el período seleccionado.", normalFont
                ));
            }

            // ---- Tabla de registros ----
            if (!records.isEmpty()) {
                document.add(new Paragraph("\nHistorial de mediciones", headerFont));
                document.add(new Paragraph(" "));

                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{2.5f, 1.5f, 1.5f, 1.5f, 1.5f});

                // Cabeceras de la tabla
                String[] headers = {
                    "Fecha y hora", "FC (bpm)", "SpO2 (%)", "Pasos", "Temp. (°C)"
                };
                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                    cell.setBackgroundColor(new BaseColor(26, 140, 122));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setPadding(5);
                    table.addCell(cell);
                }

                // Filas de datos
                for (BiometricRecord r : records) {
                    table.addCell(new PdfPCell(new Phrase(
                            r.getTimestamp().format(FORMATTER), normalFont
                    )));
                    table.addCell(cellValue(
                            r.getHeartRate(), "%.1f", normalFont
                    ));
                    table.addCell(cellValue(
                            r.getSpO2(), "%.1f", normalFont
                    ));
                    table.addCell(new PdfPCell(new Phrase(
                            r.getSteps() != null ? r.getSteps().toString() : "—",
                            normalFont
                    )));
                    table.addCell(cellValue(
                            r.getTemperature(), "%.1f", normalFont
                    ));
                }

                document.add(table);
            }

            // ---- Pie de página ----
            document.add(new Paragraph(
                    "\nGenerado por Khelder — " +
                    LocalDateTime.now().format(FORMATTER),
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8,
                            BaseColor.GRAY)
            ));

            document.close();

        } catch (Exception e) {
            log.error("Error generando PDF", e);
            throw new RuntimeException("Error generando el informe PDF", e);
        }

        return out.toByteArray();
    }

    // -------------------------------------------------------------------------
    // RF-16: Generación de CSV
    // -------------------------------------------------------------------------

    private byte[] generateCsv(
            Patient               patient,
            List<BiometricRecord> records
    ) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (CSVPrinter printer = new CSVPrinter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder()
                        .setHeader(
                            "fecha_hora", "fc_bpm", "spo2_pct",
                            "pasos", "temperatura_c", "device_id"
                        )
                        .build()
        )) {
            for (BiometricRecord r : records) {
                printer.printRecord(
                        r.getTimestamp().format(FORMATTER),
                        r.getHeartRate() != null
                                ? String.format("%.1f", r.getHeartRate()) : "",
                        r.getSpO2() != null
                                ? String.format("%.1f", r.getSpO2()) : "",
                        r.getSteps() != null ? r.getSteps() : "",
                        r.getTemperature() != null
                                ? String.format("%.1f", r.getTemperature()) : "",
                        r.getDeviceId()
                );
            }
        } catch (IOException e) {
            log.error("Error generando CSV", e);
            throw new RuntimeException("Error generando el informe CSV", e);
        }

        return out.toByteArray();
    }

    // -------------------------------------------------------------------------
    // Utilidades
    // -------------------------------------------------------------------------

    private PdfPCell cellValue(
            Double value,
            String format,
            Font   font
    ) {
        String text = value != null ? String.format(format, value) : "—";
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private UUID getAuthenticatedCaregiverId() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return caregiverRepository.findByEmail(email)
                .orElseThrow()
                .getCaregiverId();
    }
}