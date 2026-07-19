package com.smart.transformer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Uploads/signs files against Supabase Storage's REST API.
 * Uses the service-role key (set on the shared WebClient bean) so it bypasses RLS —
 * this class must only ever be called from the backend, never exposed to the frontend directly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupabaseStorageService {

    private final WebClient supabaseWebClient;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.storage-bucket}")
    private String bucket;

    /**
     * Uploads bytes to a private bucket under a report-type folder
     * (e.g. "daily/", "manual/", "critical/", "weekly/", "monthly/") and returns the
     * storage object path (NOT a public URL — this bucket should be private for sensitive reports).
     *
     * @param folder   report-type subfolder within the bucket, e.g. "manual" or "critical"
     * @param fileName leaf file name, e.g. "TX-001-1692123456789.pdf"
     */
    public String uploadPdf(String folder, String fileName, byte[] pdfBytes) {
        String objectPath = folder + "/" + fileName;

        supabaseWebClient.put()
                .uri("/storage/v1/object/{bucket}/{path}", bucket, objectPath)
                .contentType(MediaType.APPLICATION_PDF)
                .bodyValue(pdfBytes)
                .retrieve()
                .toBodilessEntity()
                .block();

        return objectPath;
    }

    /**
     * Generates a short-lived signed URL so the frontend can download a private object.
     * Called on demand (never persisted) — the DB only stores the object path.
     */
    public String generateSignedUrl(String objectPath, int expiresInSeconds) {
        SignRequest body = new SignRequest(expiresInSeconds);

        SignResponse response = supabaseWebClient.post()
                .uri("/storage/v1/object/sign/{bucket}/{path}", bucket, objectPath)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(SignResponse.class)
                .block();

        return response != null ? supabaseUrl + "/storage/v1" + response.signedURL() : null;
    }

    /**
     * Deletes an object from storage — used by the report retention/cleanup job
     * when purging reports past their configured retention period.
     */
    public void deleteObject(String objectPath) {
        try {
            supabaseWebClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri("/storage/v1/object/{bucket}/{path}", bucket, objectPath)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.warn("Failed to delete storage object {}: {}", objectPath, e.getMessage());
        }
    }

    private record SignRequest(int expiresIn) {}
    private record SignResponse(String signedURL) {}
}
