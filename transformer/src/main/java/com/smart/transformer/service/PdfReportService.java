package com.smart.transformer.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.IBlockElement;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.smart.transformer.dto.response.SensorReadingResponse;
import com.smart.transformer.entity.Alert;
import com.smart.transformer.entity.MaintenanceRecord;
import com.smart.transformer.entity.Transformer;
import com.smart.transformer.entity.enums.AlertSeverity;
import com.smart.transformer.entity.enums.TransformerStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the PDF content for transformer health reports (manual / daily / critical / weekly / monthly).
 * Pure PDF-building logic lives here — uploading to Supabase and persisting metadata is
 * handled by {@code ReportService}, which composes this with {@code SupabaseStorageService}.
 *
 * <p>Visual design: a card-style KPI summary, colored status/severity badges, zebra-striped
 * data tables, and accent-bordered "callout" boxes for the narrative sections — aimed at
 * being easy to scan for a field engineer rather than a plain text dump.
 */
@Service
public class PdfReportService {

    private static final DeviceRgb BRAND_COLOR = new DeviceRgb(0x1E, 0x40, 0xAF);       // indigo
    private static final DeviceRgb BRAND_TINT = new DeviceRgb(0xEE, 0xF1, 0xFB);        // very light indigo
    private static final DeviceRgb CRITICAL_COLOR = new DeviceRgb(0xC0, 0x39, 0x2B);
    private static final DeviceRgb CRITICAL_TINT = new DeviceRgb(0xFC, 0xEC, 0xEA);
    private static final DeviceRgb WARNING_COLOR = new DeviceRgb(0xB7, 0x7B, 0x0B);
    private static final DeviceRgb WARNING_TINT = new DeviceRgb(0xFD, 0xF5, 0xE3);
    private static final DeviceRgb HEALTHY_COLOR = new DeviceRgb(0x1E, 0x8E, 0x3E);
    private static final DeviceRgb HEALTHY_TINT = new DeviceRgb(0xEA, 0xF7, 0xEC);
    private static final DeviceRgb OFFLINE_COLOR = new DeviceRgb(0x75, 0x75, 0x75);
    private static final DeviceRgb INFO_COLOR = new DeviceRgb(0x2F, 0x6F, 0xED);
    private static final DeviceRgb TABLE_HEADER_BG = new DeviceRgb(0x2B, 0x2F, 0x3A);
    private static final DeviceRgb ROW_STRIPE_BG = new DeviceRgb(0xF6, 0xF7, 0xFA);
    private static final DeviceRgb MUTED_TEXT = new DeviceRgb(0x6B, 0x72, 0x80);
    private static final DeviceRgb DIVIDER = new DeviceRgb(0xE3, 0xE6, 0xEC);

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm 'UTC'")
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

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

            document.setMargins(28, 32, 28, 32);

            addHeader(document, transformer, reportTitle);
            addKpiSummary(document, transformer, readings, alerts);
            addTransformerDetails(document, transformer);
            addSensorReadingsSection(document, readings);
            addAlertSummarySection(document, alerts);
            addHealthAnalysisSection(document, transformer, readings, alerts);
            addMaintenanceRecommendationsSection(document, transformer, maintenanceRecords);
            addFooter(document, reportTitle);

        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(
                    "Failed to build PDF report for " + transformer.getAssetTag(), e);
        }

        return baos.toByteArray();
    }

    // ---------- header / banner ----------

    private void addHeader(Document document, Transformer transformer, String reportTitle) {
        Table banner = new Table(UnitValue.createPercentArray(new float[]{1, 3.2f, 1.4f})).useAllAvailableWidth();
        banner.setBorder(Border.NO_BORDER);

        // Monogram "logo" block
        Cell logoCell = new Cell()
                .add(new Paragraph("THMP").setBold().setFontSize(16).setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(BRAND_COLOR)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(Border.NO_BORDER)
                .setHeight(56);

        // Title / subtitle block
        Cell titleCell = new Cell()
                .add(new Paragraph("Transformer Health Monitoring Platform")
                        .setBold().setFontSize(15).setFontColor(new DeviceRgb(0x1A, 0x1D, 0x26))
                        .setMarginBottom(2))
                .add(new Paragraph(transformer.getName() + "  \u2022  " + transformer.getAssetTag())
                        .setFontSize(10).setFontColor(MUTED_TEXT))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(BRAND_TINT)
                .setBorder(Border.NO_BORDER)
                .setPaddingLeft(14);

        // Report-type pill + generated date
        Cell metaCell = new Cell()
                .add(reportTypePill(reportTitle))
                .add(new Paragraph(TS_FMT.format(Instant.now())).setFontSize(8).setFontColor(MUTED_TEXT)
                        .setTextAlignment(TextAlignment.RIGHT).setMarginTop(6))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBackgroundColor(BRAND_TINT)
                .setBorder(Border.NO_BORDER)
                .setPaddingRight(10);

        banner.addCell(logoCell);
        banner.addCell(titleCell);
        banner.addCell(metaCell);
        document.add(banner);

        document.add(new Div().setHeight(3).setBackgroundColor(BRAND_COLOR).setMarginBottom(16));
    }

    private Paragraph reportTypePill(String reportTitle) {
        return new Paragraph(reportTitle.toUpperCase())
                .setBold().setFontSize(9).setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(BRAND_COLOR)
                .setPadding(5)
                .setTextAlignment(TextAlignment.CENTER);
    }

    // ---------- KPI summary cards ----------

    private void addKpiSummary(Document document, Transformer t, List<SensorReadingResponse> readings, List<Alert> alerts) {
        DeviceRgb accent = statusColor(t.getStatus());
        Double score = t.getHealthScore();
        SensorReadingResponse latest = (readings != null && !readings.isEmpty()) ? readings.get(0) : null;
        long criticalAlerts = alerts == null ? 0 : alerts.stream().filter(a -> a.getSeverity() == AlertSeverity.CRITICAL).count();
        long totalAlerts = alerts == null ? 0 : alerts.size();

        Table cards = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1})).useAllAvailableWidth();
        cards.setMarginBottom(16);

        cards.addCell(kpiCard("HEALTH SCORE", score != null ? String.format("%.0f/100", score) : "N/A", accent));
        cards.addCell(kpiCard("STATUS", t.getStatus() != null ? t.getStatus().name() : "UNKNOWN", accent));
        cards.addCell(kpiCard("TEMPERATURE", latest != null ? fmt(latest.getTemperatureCelsius()) + "\u00B0C" : "N/A",
                latest != null && latest.getTemperatureCelsius() != null && latest.getTemperatureCelsius() > 75
                        ? CRITICAL_COLOR : BRAND_COLOR));
        cards.addCell(kpiCard("ALERTS (TOTAL / CRITICAL)", totalAlerts + " / " + criticalAlerts,
                criticalAlerts > 0 ? CRITICAL_COLOR : HEALTHY_COLOR));

        document.add(cards);
    }

    private Cell kpiCard(String label, String value, DeviceRgb accent) {
        Div content = new Div()
                .add(new Paragraph(label).setFontSize(8).setBold().setFontColor(MUTED_TEXT).setMarginBottom(4))
                .add(new Paragraph(value).setFontSize(17).setBold().setFontColor(new DeviceRgb(0x1A, 0x1D, 0x26)))
                .setPadding(10);

        return new Cell()
                .add(content)
                .setBorderTop(new SolidBorder(accent, 3))
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(Border.NO_BORDER)
                .setBackgroundColor(ColorConstants.WHITE)
                .setMarginRight(6);
    }

    // ---------- section header helper ----------

    private Div sectionTitle(String text) {
        return new Div()
                .add(new Paragraph(text).setBold().setFontSize(12.5f).setFontColor(new DeviceRgb(0x1A, 0x1D, 0x26)))
                .setBorderLeft(new SolidBorder(BRAND_COLOR, 3))
                .setPaddingLeft(8)
                .setMarginTop(4)
                .setMarginBottom(8);
    }

    // ---------- transformer details ----------

    private void addTransformerDetails(Document document, Transformer t) {
        document.add(sectionTitle("Transformer Details"));

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1})).useAllAvailableWidth();
        table.setMarginBottom(16);
        addInfoCell(table, "Asset Tag", t.getAssetTag(), true);
        addInfoCell(table, "Name", t.getName(), false);
        addInfoCell(table, "Location", t.getLocation() != null ? t.getLocation() : "N/A", true);
        addInfoCell(table, "Capacity (kVA)", t.getCapacityKva() != null ? t.getCapacityKva().toString() : "N/A", false);
        addInfoCell(table, "Installed", t.getInstallationDate() != null ? fmtDate(t.getInstallationDate()) : "N/A", true);
        addInfoCell(table, "Status", null, false, statusBadge(t.getStatus()));
        document.add(table);
    }

    private void addInfoCell(Table table, String label, String value, boolean shaded) {
        addInfoCell(table, label, value, shaded, null);
    }

    private void addInfoCell(Table table, String label, String value, boolean shaded, IBlockElement valueOverride) {
        Div content = new Div()
                .add(new Paragraph(label.toUpperCase()).setFontSize(7.5f).setBold().setFontColor(MUTED_TEXT).setMarginBottom(2));
        if (valueOverride != null) {
            content.add(valueOverride);
        } else {
            content.add(new Paragraph(value != null ? value : "-").setFontSize(10.5f).setFontColor(new DeviceRgb(0x1A, 0x1D, 0x26)));
        }
        table.addCell(new Cell().add(content)
                .setBackgroundColor(shaded ? ROW_STRIPE_BG : ColorConstants.WHITE)
                .setBorder(new SolidBorder(DIVIDER, 0.5f))
                .setPadding(8));
    }

    // ---------- sensor readings ----------

    private void addSensorReadingsSection(Document document, List<SensorReadingResponse> readings) {
        document.add(sectionTitle("Sensor Readings"));

        if (readings == null || readings.isEmpty()) {
            document.add(emptyState("No sensor readings available for this transformer yet."));
            return;
        }

        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 1.1f, 1.1f, 1.1f, 1.1f, 1.1f}))
                .useAllAvailableWidth();
        table.setMarginBottom(16);
        addHeaderCell(table, "Recorded At");
        addHeaderCell(table, "Temp (\u00B0C)");
        addHeaderCell(table, "Voltage (V)");
        addHeaderCell(table, "Current (A)");
        addHeaderCell(table, "Vibration (mm)");
        addHeaderCell(table, "Anomaly Score");

        int[] i = {0};
        readings.stream().limit(20).forEach(r -> {
            boolean shaded = i[0]++ % 2 == 1;
            table.addCell(rowCell(fmtInstant(r.getRecordedAt()), shaded, TextAlignment.LEFT));
            table.addCell(rowCell(fmt(r.getTemperatureCelsius()), shaded, TextAlignment.CENTER));
            table.addCell(rowCell(fmt(r.getVoltageVolts()), shaded, TextAlignment.CENTER));
            table.addCell(rowCell(fmt(r.getLoadCurrentAmps()), shaded, TextAlignment.CENTER));
            table.addCell(rowCell(fmt(r.getVibrationMm()), shaded, TextAlignment.CENTER));
            table.addCell(rowCell(fmt(r.getAnomalyScore()), shaded, TextAlignment.CENTER));
        });

        document.add(table);
    }

    // ---------- alert summary ----------

    private void addAlertSummarySection(Document document, List<Alert> alerts) {
        document.add(sectionTitle("Alert Summary"));

        if (alerts == null || alerts.isEmpty()) {
            document.add(emptyState("No alerts recorded for this transformer."));
            return;
        }

        Map<AlertSeverity, Long> counts = alerts.stream()
                .collect(Collectors.groupingBy(Alert::getSeverity, Collectors.counting()));

        Table countRow = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1})).useAllAvailableWidth();
        countRow.setMarginBottom(10);
        countRow.addCell(countChip("Critical", counts.getOrDefault(AlertSeverity.CRITICAL, 0L), CRITICAL_COLOR, CRITICAL_TINT));
        countRow.addCell(countChip("Warning", counts.getOrDefault(AlertSeverity.WARNING, 0L), WARNING_COLOR, WARNING_TINT));
        countRow.addCell(countChip("Info", counts.getOrDefault(AlertSeverity.INFO, 0L), INFO_COLOR, BRAND_TINT));
        document.add(countRow);

        Table table = new Table(UnitValue.createPercentArray(new float[]{1.3f, 0.9f, 1, 3.2f, 0.9f}))
                .useAllAvailableWidth();
        table.setMarginBottom(16);
        addHeaderCell(table, "Raised At");
        addHeaderCell(table, "Severity");
        addHeaderCell(table, "Source");
        addHeaderCell(table, "Message");
        addHeaderCell(table, "Ack'd");

        int[] i = {0};
        alerts.stream().limit(15).forEach(a -> {
            boolean shaded = i[0]++ % 2 == 1;
            table.addCell(rowCell(fmtInstant(a.getCreatedAt()), shaded, TextAlignment.LEFT));
            table.addCell(new Cell().add(severityBadge(a.getSeverity()))
                    .setBackgroundColor(shaded ? ROW_STRIPE_BG : ColorConstants.WHITE)
                    .setBorder(Border.NO_BORDER).setPadding(6).setTextAlignment(TextAlignment.CENTER));
            table.addCell(rowCell(a.getSource().name(), shaded, TextAlignment.LEFT));
            table.addCell(rowCell(a.getMessage() != null ? a.getMessage() : "-", shaded, TextAlignment.LEFT));
            table.addCell(rowCell(a.isAcknowledged() ? "Yes" : "No", shaded, TextAlignment.CENTER));
        });

        document.add(table);
    }

    private Cell countChip(String label, long count, DeviceRgb color, DeviceRgb tint) {
        Div content = new Div()
                .add(new Paragraph(String.valueOf(count)).setBold().setFontSize(16).setFontColor(color).setMarginBottom(0))
                .add(new Paragraph(label).setFontSize(8).setFontColor(MUTED_TEXT))
                .setPadding(8);
        return new Cell().add(content)
                .setBackgroundColor(tint)
                .setBorder(Border.NO_BORDER)
                .setMarginRight(6);
    }

    // ---------- narrative / callout sections ----------

    private void addHealthAnalysisSection(Document document, Transformer t,
                                           List<SensorReadingResponse> readings, List<Alert> alerts) {
        document.add(sectionTitle("Health Analysis"));
        document.add(calloutBox(buildHealthAnalysis(t, readings, alerts), BRAND_COLOR, BRAND_TINT));
    }

    private void addMaintenanceRecommendationsSection(Document document, Transformer t, List<MaintenanceRecord> records) {
        document.add(sectionTitle("Maintenance Recommendations"));
        DeviceRgb accent = t.getStatus() == TransformerStatus.CRITICAL ? CRITICAL_COLOR
                : t.getStatus() == TransformerStatus.WARNING ? WARNING_COLOR : HEALTHY_COLOR;
        DeviceRgb tint = t.getStatus() == TransformerStatus.CRITICAL ? CRITICAL_TINT
                : t.getStatus() == TransformerStatus.WARNING ? WARNING_TINT : HEALTHY_TINT;
        document.add(calloutBox(buildMaintenanceRecommendation(t, records), accent, tint));

        if (records != null && !records.isEmpty()) {
            document.add(new Paragraph("Recent Maintenance History").setBold().setFontSize(10.5f)
                    .setFontColor(new DeviceRgb(0x1A, 0x1D, 0x26)).setMarginTop(12).setMarginBottom(6));

            Table table = new Table(UnitValue.createPercentArray(new float[]{1.3f, 3.4f, 1.5f, 1.3f})).useAllAvailableWidth();
            addHeaderCell(table, "Performed At");
            addHeaderCell(table, "Description");
            addHeaderCell(table, "Performed By");
            addHeaderCell(table, "Next Due");

            int[] i = {0};
            records.stream().limit(10).forEach(m -> {
                boolean shaded = i[0]++ % 2 == 1;
                table.addCell(rowCell(fmtInstant(m.getPerformedAt()), shaded, TextAlignment.LEFT));
                table.addCell(rowCell(m.getDescription() != null ? m.getDescription() : "-", shaded, TextAlignment.LEFT));
                table.addCell(rowCell(m.getPerformedBy() != null ? m.getPerformedBy() : "-", shaded, TextAlignment.LEFT));
                table.addCell(rowCell(m.getNextDueAt() != null ? fmtInstant(m.getNextDueAt()) : "-", shaded, TextAlignment.LEFT));
            });
            document.add(table);
        }
    }

    private Div calloutBox(String text, DeviceRgb accent, DeviceRgb tint) {
        return new Div()
                .add(new Paragraph(text).setFontSize(10).setFontColor(new DeviceRgb(0x33, 0x36, 0x40)))
                .setBackgroundColor(tint)
                .setBorderLeft(new SolidBorder(accent, 3))
                .setPadding(12)
                .setMarginBottom(16);
    }

    private Div emptyState(String text) {
        return new Div()
                .add(new Paragraph(text).setFontSize(9.5f).setFontColor(MUTED_TEXT).setItalic())
                .setBackgroundColor(ROW_STRIPE_BG)
                .setPadding(10)
                .setMarginBottom(16);
    }

    // ---------- footer ----------

    private void addFooter(Document document, String reportTitle) {
        document.add(new Div().setHeight(1).setBackgroundColor(DIVIDER).setMarginTop(4).setMarginBottom(8));

        Table footer = new Table(UnitValue.createPercentArray(new float[]{2, 1})).useAllAvailableWidth();
        footer.addCell(new Cell()
                .add(new Paragraph("Transformer Health Monitoring Platform \u2014 " + reportTitle)
                        .setFontSize(8).setFontColor(MUTED_TEXT))
                .setBorder(Border.NO_BORDER));
        footer.addCell(new Cell()
                .add(new Paragraph("Generated " + TS_FMT.format(Instant.now()))
                        .setFontSize(8).setFontColor(MUTED_TEXT).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER));
        document.add(footer);
        document.add(new Paragraph("This report was generated automatically. Download links expire after 1 hour — "
                + "request a fresh link from the Reports dashboard if needed.")
                .setFontSize(7.5f).setFontColor(MUTED_TEXT).setMarginTop(4));
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
                sb.append(" Next scheduled maintenance is due at ").append(fmtInstant(latest.getNextDueAt())).append(".");
            }
        }
        return sb.toString();
    }

    // ---------- badges ----------

    private Paragraph severityBadge(AlertSeverity severity) {
        DeviceRgb color = switch (severity) {
            case CRITICAL -> CRITICAL_COLOR;
            case WARNING -> WARNING_COLOR;
            case INFO -> INFO_COLOR;
        };
        return new Paragraph(severity.name())
                .setBold().setFontSize(7.5f).setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(color)
                .setPadding(3)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private Paragraph statusBadge(TransformerStatus status) {
        DeviceRgb color = statusColor(status);
        return new Paragraph(status != null ? status.name() : "UNKNOWN")
                .setBold().setFontSize(9).setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(color)
                .setPadding(4)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private DeviceRgb statusColor(TransformerStatus status) {
        if (status == null) return OFFLINE_COLOR;
        return switch (status) {
            case CRITICAL -> CRITICAL_COLOR;
            case WARNING -> WARNING_COLOR;
            case HEALTHY -> HEALTHY_COLOR;
            case OFFLINE -> OFFLINE_COLOR;
        };
    }

    // ---------- table cell helpers ----------

    private void addHeaderCell(Table table, String text) {
        table.addHeaderCell(new Cell()
                .add(new Paragraph(text).setBold().setFontSize(8.5f).setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(TABLE_HEADER_BG)
                .setBorder(Border.NO_BORDER)
                .setPadding(6));
    }

    private Cell rowCell(String text, boolean shaded, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "-").setFontSize(8.5f)
                        .setFontColor(new DeviceRgb(0x33, 0x36, 0x40)))
                .setBackgroundColor(shaded ? ROW_STRIPE_BG : ColorConstants.WHITE)
                .setBorder(Border.NO_BORDER)
                .setPadding(6)
                .setTextAlignment(alignment);
    }

    // ---------- formatting helpers ----------

    private String fmtInstant(Instant instant) {
        return instant != null ? TS_FMT.format(instant) : "-";
    }

    private String fmtDate(LocalDate date) {
        return date != null ? DATE_FMT.format(date) : "-";
    }

    private String fmt(Double value) {
        return value != null ? String.format("%.2f", value) : "-";
    }
}
