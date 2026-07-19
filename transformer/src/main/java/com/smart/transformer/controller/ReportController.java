package com.smart.transformer.controller;

import com.smart.transformer.dto.response.ApiResponse;
import com.smart.transformer.entity.Transformer;
import com.smart.transformer.service.PdfReportService;
import com.smart.transformer.service.SensorReadingService;
import com.smart.transformer.service.TransformerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final PdfReportService pdfReportService;
    private final TransformerService transformerService;
    private final SensorReadingService sensorReadingService;

    /**
     * Generates a PDF health report for a transformer, stores it in Supabase Storage,
     * and returns a signed download URL.
     */
    @PostMapping("/transformer/{transformerId}")
    public ApiResponse<String> generateReport(@PathVariable Long transformerId) {
        Transformer transformer = transformerService.getEntity(transformerId);
        var readings = sensorReadingService.getHistory(transformerId, PageRequest.of(0, 50)).getContent();

        String signedUrl = pdfReportService.generateHealthReport(transformer, readings);
        return ApiResponse.success("Report generated", signedUrl);
    }
}
