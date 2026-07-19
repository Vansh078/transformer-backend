package com.smart.transformer.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.smart.transformer.dto.response.SensorReadingResponse;
import com.smart.transformer.entity.Transformer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final SupabaseStorageService supabaseStorageService;

    /**
     * Builds a health report PDF for a transformer, uploads it to Supabase Storage,
     * and returns a signed URL the frontend/email can use to download it.
     */
    public String generateHealthReport(Transformer transformer, List<SensorReadingResponse> readings) {
        byte[] pdfBytes = buildPdf(transformer, readings);

        String fileName = transformer.getAssetTag() + "-" + Instant.now().toEpochMilli() + ".pdf";
        String objectPath = supabaseStorageService.uploadPdf(fileName, pdfBytes);

        return supabaseStorageService.generateSignedUrl(objectPath, 3600); // 1 hour link
    }

    private byte[] buildPdf(Transformer transformer, List<SensorReadingResponse> readings) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            document.add(new Paragraph("Transformer Health Report").setBold().setFontSize(18));
            document.add(new Paragraph(transformer.getName() + " (" + transformer.getAssetTag() + ")"));
            document.add(new Paragraph("Location: " + (transformer.getLocation() != null ? transformer.getLocation() : "N/A")));
            document.add(new Paragraph("Status: " + transformer.getStatus()));
            document.add(new Paragraph("Health Score: " + (transformer.getHealthScore() != null ? transformer.getHealthScore() : "N/A")));
            document.add(new Paragraph("Generated: " + DateTimeFormatter.ISO_INSTANT.format(Instant.now())));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Recent Sensor Readings").setBold().setFontSize(14));

            Table table = new Table(UnitValue.createPercentArray(new float[]{2, 2, 2, 2, 2, 2}))
                    .useAllAvailableWidth();
            table.addHeaderCell("Recorded At");
            table.addHeaderCell("Temp (C)");
            table.addHeaderCell("Oil %");
            table.addHeaderCell("Vibration");
            table.addHeaderCell("Load (A)");
            table.addHeaderCell("Anomaly Score");

            for (SensorReadingResponse r : readings) {
                table.addCell(String.valueOf(r.getRecordedAt()));
                table.addCell(fmt(r.getTemperatureCelsius()));
                table.addCell(fmt(r.getVibrationMm()));
                table.addCell(fmt(r.getLoadCurrentAmps()));
                table.addCell(fmt(r.getAnomalyScore()));
            }

            document.add(table);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException("Failed to build PDF report for " + transformer.getAssetTag(), e);
        }

        return baos.toByteArray();
    }

    private String fmt(Double value) {
        return value != null ? String.format("%.2f", value) : "-";
    }
}
