package com.smart.transformer.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.smart.transformer.dto.response.SensorReadingResponse;
import com.smart.transformer.entity.Alert;
import com.smart.transformer.entity.MaintenanceRecord;
import com.smart.transformer.entity.Transformer;
import com.smart.transformer.entity.enums.AlertSeverity;
import com.smart.transformer.entity.enums.TransformerStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the PDF content for transformer health reports (manual / daily / critical / weekly / monthly).
 * Pure PDF-building logic lives here — uploading to Supabase and persisting metadata is
 * handled by {@code ReportService}, which composes this with {@code SupabaseStorageService}.
 */
@Service
public class PdfReportService {

    private static final DeviceRgb BRAND_COLOR = new DeviceRgb(0x1E, 0x40, 0xAF); // indigo
    private static final DeviceRgb CRITICAL_COLOR = new DeviceRgb(0xC0, 0x39, 0x2B);
    private static final DeviceRgb WARNING_COLOR = new DeviceRgb(0xB7, 0x95, 0x0B);
    private static final DeviceRgb HEALTHY_COLOR = new DeviceRgb(0x1E, 0x8E, 0x3E);
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
            .withZone(ZoneOffset.UTC);

    /**
     * Builds a full transformer health report PDF.
     *
     * @param transformer         the transformer this report is about
     * @param readings            recent sensor readings, most-recent first
     * @param alerts              recent alerts for this transformer (any severity)
     * @param maintenanceRecords  maintenance history for this transformer
     * @param reportTitle         e.g. "Manual Health Report", "Daily Health Report", "Critical Health Report"
     */
    public byte[] buildTransformerReport(Transformer transformer,
                                          List<SensorReadingResponse> readings,
                                          List<Alert> alerts,
                                          List<MaintenanceRecord> maintenanceRecords,
                                          String reportTitle) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            addHeader(document, reportTitle);
            addTransformerDetails(document, transformer);
            addHealthScoreSection(document, transformer);
            addSensorReadingsSection(document, readings);
            addAlertSummarySection(document, alerts);
            addHealthAnalysisSection(document, transformer, readings, alerts);
            addMaintenanceRecommendationsSection(document, transformer, maintenanceRecords);
            addFooter(document);

        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(
                    "Failed to build PDF report for " + transformer.getAssetTag(), e);
        }

        return baos.toByteArray();
    }

    // ---------- sections ----------

    private void addHeader(Document document, String reportTitle) {
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 3})).useAllAvailableWidth();

        Cell logoCell = new Cell()
                .add(new Paragraph("TH").setBold().setFontSize(22).setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(BRAND_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(Border.NO_BORDER)
                .setPadding(10);
        Cell titleCell = new Cell()
                .add(new Paragraph("Transformer Health Monitoring Platform").setBold().setFontSize(16))
                .add(new Paragraph(reportTitle).setFontSize(13).setFontColor(BRAND_COLOR))
                .setBorder(Border.NO_BORDER)
                .setPaddingLeft(12);

        headerTable.addCell(logoCell);
        headerTable.addCell(titleCell);
        document.add(headerTable);
        document.add(new Paragraph(" "));
    }

    private void addTransformerDetails(Document document, Transformer t) {
        document.add(sectionTitle("Transformer Details"));

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1})).useAllAvailableWidth();
        addLabelValue(table, "Asset Tag", t.getAssetTag());
        addLabelValue(table, "Name", t.getName());
        addLabelValue(table, "Location", t.getLocation() != null ? t.getLocation() : "N/A");
        addLabelValue(table, "Status", t.getStatus() != null ? t.getStatus().name() : "N/A");
        addLabelValue(table, "Capacity (kVA)", t.getCapacityKva() != null ? t.getCapacityKva().toString() : "N/A");
        addLabelValue(table, "Installed", t.getInstallationDate() != null ? t.getInstallationDate().toString() : "N/A");
        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addHealthScoreSection(Document document, Transformer t) {
        document.add(sectionTitle("Current Health Score"));

        Double score = t.getHealthScore();
        DeviceRgb color = statusColor(t.getStatus());

        Div scoreDiv = new Div()
                .add(new Paragraph(score != null ? String.format("%.1f / 100", score) : "N/A")
                        .setBold().setFontSize(28).setFontColor(color))
                .add(new Paragraph("Status: " + (t.getStatus() != null ? t.getStatus() : "UNKNOWN"))
                        .setFontColor(color).setBold());
        document.add(scoreDiv);
        document.add(new Paragraph(" "));
    }

    private void addSensorReadingsSection(Document document, List<SensorReadingResponse> readings) {
        document.add(sectionTitle("Sensor Readings (Temperature, Voltage, Current, Vibration)"));

        if (readings == null || readings.isEmpty()) {
            document.add(new Paragraph("No sensor readings available for this transformer yet."));
            document.add(new Paragraph(" "));
            return;
        }

        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f}))
                .useAllAvailableWidth();
        addHeaderCell(table, "Recorded At");
        addHeaderCell(table, "Temp (\u00B0C)");
        addHeaderCell(table, "Voltage (V)");
        addHeaderCell(table, "Current (A)");
        addHeaderCell(table, "Vibration (mm)");
        addHeaderCell(table, "Anomaly Score");

        // Most recent 20 readings keep the report readable
        readings.stream().limit(20).forEach(r -> {
            table.addCell(cell(String.valueOf(r.getRecordedAt())));
            table.addCell(cell(fmt(r.getTemperatureCelsius())));
            table.addCell(cell(fmt(r.getVoltageVolts())));
            table.addCell(cell(fmt(r.getLoadCurrentAmps())));
            table.addCell(cell(fmt(r.getVibrationMm())));
            table.addCell(cell(fmt(r.getAnomalyScore())));
        });

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addAlertSummarySection(Document document, List<Alert> alerts) {
        document.add(sectionTitle("Alert Summary"));

        if (alerts == null || alerts.isEmpty()) {
            document.add(new Paragraph("No alerts recorded for this transformer."));
            document.add(new Paragraph(" "));
            return;
        }

        Map<AlertSeverity, Long> counts = alerts.stream()
                .collect(Collectors.groupingBy(Alert::getSeverity, Collectors.counting()));

        Paragraph countsPara = new Paragraph();
        countsPara.add("Critical: " + counts.getOrDefault(AlertSeverity.CRITICAL, 0L) + "   ");
        countsPara.add("Warning: " + counts.getOrDefault(AlertSeverity.WARNING, 0L) + "   ");
        countsPara.add("Info: " + counts.getOrDefault(AlertSeverity.INFO, 0L));
        document.add(countsPara.setBold());

        Table table = new Table(UnitValue.createPercentArray(new float[]{1.2f, 1, 1, 3, 1}))
                .useAllAvailableWidth();
        addHeaderCell(table, "Raised At");
        addHeaderCell(table, "Severity");
        addHeaderCell(table, "Source");
        addHeaderCell(table, "Message");
        addHeaderCell(table, "Acknowledged");

        alerts.stream().limit(15).forEach(a -> {
            table.addCell(cell(String.valueOf(a.getCreatedAt())));
            table.addCell(cell(a.getSeverity().name()));
            table.addCell(cell(a.getSource().name()));
            table.addCell(cell(a.getMessage() != null ? a.getMessage() : "-"));
            table.addCell(cell(a.isAcknowledged() ? "Yes" : "No"));
        });

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addHealthAnalysisSection(Document document, Transformer t,
                                           List<SensorReadingResponse> readings, List<Alert> alerts) {
        document.add(sectionTitle("Health Analysis"));
        document.add(new Paragraph(buildHealthAnalysis(t, readings, alerts)));
        document.add(new Paragraph(" "));
    }

    private void addMaintenanceRecommendationsSection(Document document, Transformer t, List<MaintenanceRecord> records) {
        document.add(sectionTitle("Maintenance Recommendations"));
        document.add(new Paragraph(buildMaintenanceRecommendation(t, records)));

        if (records != null && !records.isEmpty()) {
            document.add(new Paragraph("Recent Maintenance History:").setBold());
            Table table = new Table(UnitValue.createPercentArray(new float[]{1.2f, 3, 1.5f, 1.2f})).useAllAvailableWidth();
            addHeaderCell(table, "Performed At");
            addHeaderCell(table, "Description");
            addHeaderCell(table, "Performed By");
            addHeaderCell(table, "Next Due");

            records.stream().limit(10).forEach(m -> {
                table.addCell(cell(String.valueOf(m.getPerformedAt())));
                table.addCell(cell(m.getDescription() != null ? m.getDescription() : "-"));
                table.addCell(cell(m.getPerformedBy() != null ? m.getPerformedBy() : "-"));
                table.addCell(cell(m.getNextDueAt() != null ? m.getNextDueAt().toString() : "-"));
            });
            document.add(table);
        }
        document.add(new Paragraph(" "));
    }

    private void addFooter(Document document) {
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Report generated: " + TS_FMT.format(Instant.now()))
                .setFontSize(9).setFontColor(ColorConstants.GRAY));
        document.add(new Paragraph("This report was generated automatically by the Transformer Health Monitoring Platform.")
                .setFontSize(9).setFontColor(ColorConstants.GRAY));
    }

    // ---------- rule-based narrative generation (no external AI dependency, always available) ----------

    private String buildHealthAnalysis(Transformer t, List<SensorReadingResponse> readings, List<Alert> alerts) {
        Double score = t.getHealthScore();
        TransformerStatus status = t.getStatus();
        long criticalAlerts = alerts == null ? 0 : alerts.stream().filter(a -> a.getSeverity() == AlertSeverity.CRITICAL).count();
        long warningAlerts = alerts == null ? 0 : alerts.stream().filter(a -> a.getSeverity() == AlertSeverity.WARNING).count();

        StringBuilder sb = new StringBuilder();
        sb.append(t.getName()).append(" (").append(t.getAssetTag()).append(") currently has a health score of ")
                .append(score != null ? String.format("%.1f/100", score) : "an unknown value")
                .append(" and is in ").append(status != null ? status.name() : "an unknown").append(" status. ");

        if (status == TransformerStatus.CRITICAL) {
            sb.append("This transformer requires immediate attention — readings indicate a significant deviation ")
                    .append("from normal operating parameters. ");
        } else if (status == TransformerStatus.WARNING) {
            sb.append("This transformer is showing early signs of stress and should be monitored closely over ")
                    .append("the coming days. ");
        } else if (status == TransformerStatus.HEALTHY) {
            sb.append("This transformer is operating within normal parameters. ");
        } else {
            sb.append("This transformer's connectivity status is offline; recent data may be unavailable. ");
        }

        if (criticalAlerts > 0 || warningAlerts > 0) {
            sb.append("In the period covered by this report, it raised ").append(criticalAlerts)
                    .append(" critical and ").append(warningAlerts).append(" warning alert(s). ");
        } else {
            sb.append("No warning or critical alerts were raised in the period covered by this report. ");
        }

        if (readings != null && !readings.isEmpty()) {
            SensorReadingResponse latest = readings.get(0);
            sb.append("Latest reading: temperature ").append(fmt(latest.getTemperatureCelsius())).append("\u00B0C, ")
                    .append("voltage ").append(fmt(latest.getVoltageVolts())).append("V, ")
                    .append("current ").append(fmt(latest.getLoadCurrentAmps())).append("A, ")
                    .append("vibration ").append(fmt(latest.getVibrationMm())).append("mm.");
        } else {
            sb.append("No recent sensor readings were available to analyze.");
        }

        return sb.toString();
    }

    private String buildMaintenanceRecommendation(Transformer t, List<MaintenanceRecord> records) {
        TransformerStatus status = t.getStatus();
        StringBuilder sb = new StringBuilder();

        if (status == TransformerStatus.CRITICAL) {
            sb.append("Recommendation: Schedule an emergency inspection within 24 hours. Prioritize checking ")
                    .append("cooling systems, insulation integrity, and load levels. Consider temporary load ")
                    .append("shedding if abnormal temperature or vibration persists.");
        } else if (status == TransformerStatus.WARNING) {
            sb.append("Recommendation: Schedule a preventive maintenance visit within the next 1-2 weeks. ")
                    .append("Review recent sensor trends for gradual degradation before it escalates.");
        } else if (status == TransformerStatus.HEALTHY) {
            sb.append("Recommendation: Continue routine maintenance on the standard schedule. No immediate ")
                    .append("action required based on current readings.");
        } else {
            sb.append("Recommendation: Investigate device connectivity — this transformer has not reported ")
                    .append("recent sensor data. Dispatch a technician to verify device status.");
        }

        if (records == null || records.isEmpty()) {
            sb.append(" No maintenance history is on file for this transformer; consider establishing a baseline ")
                    .append("inspection.");
        } else {
            MaintenanceRecord latest = records.get(0);
            if (latest.getNextDueAt() != null) {
                sb.append(" Next scheduled maintenance is due at ").append(latest.getNextDueAt()).append(".");
            }
        }
        return sb.toString();
    }

    // ---------- helpers ----------

    private DeviceRgb statusColor(TransformerStatus status) {
        if (status == null) return new DeviceRgb(0x33, 0x33, 0x33);
        return switch (status) {
            case CRITICAL -> CRITICAL_COLOR;
            case WARNING -> WARNING_COLOR;
            case HEALTHY -> HEALTHY_COLOR;
            case OFFLINE -> new DeviceRgb(0x75, 0x75, 0x75);
        };
    }

    private Paragraph sectionTitle(String text) {
        return new Paragraph(text).setBold().setFontSize(14).setFontColor(BRAND_COLOR);
    }

    private void addLabelValue(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold().setFontSize(10)).setBorder(Border.NO_BORDER));
        table.addCell(new Cell().add(new Paragraph(value).setFontSize(10)).setBorder(Border.NO_BORDER));
    }

    private void addHeaderCell(Table table, String text) {
        table.addHeaderCell(new Cell().add(new Paragraph(text).setBold().setFontSize(9))
                .setBackgroundColor(new DeviceRgb(0xEE, 0xEE, 0xEE)));
    }

    private Cell cell(String text) {
        return new Cell().add(new Paragraph(text != null ? text : "-").setFontSize(9));
    }

    private String fmt(Double value) {
        return value != null ? String.format("%.2f", value) : "-";
    }
}
