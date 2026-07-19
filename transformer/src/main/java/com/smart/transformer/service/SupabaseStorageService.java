package com.smart.transformer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

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
                .uri(objectUri("/storage/v1/object", objectPath))
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
                .uri(objectUri("/storage/v1/object/sign", objectPath))
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
                    .uri(objectUri("/storage/v1/object", objectPath))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.warn("Failed to delete storage object {}: {}", objectPath, e.getMessage());
        }
    }

    /**
     * Builds "{basePath}/{bucket}/{objectPath}" as a plain string rather than substituting
     * {@code objectPath} into a single {@code {path}} URI template variable.
     *
     * <p>Spring's {@code UriComponentsBuilder} percent-encodes every "/" inside a template
     * variable's <em>value</em> (turning "critical/TX-001.pdf" into "critical%2FTX-001.pdf"),
     * because a template variable is expected to represent exactly one path segment. Supabase's
     * upload and sign endpoints resolve/verify against the literal decoded object key, so an
     * upload made against the %2F-encoded key and a later sign request that resolves differently
     * (or a client hitting the real slash-separated path) fall out of sync — that mismatch is
     * what surfaces as the "InvalidSignature" / 400 error on download. Embedding the path
     * directly into the URI string (with each segment individually percent-encoded for any
     * genuinely unsafe characters) keeps "/" as a literal path separator instead.
     */
    private String objectUri(String basePath, String objectPath) {
        String encodedBucket = UriUtils.encodePathSegment(bucket, StandardCharsets.UTF_8);
        String encodedPath = Arrays.stream(objectPath.split("/"))
                .map(segment -> UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
        return basePath + "/" + encodedBucket + "/" + encodedPath;
    }

    private record SignRequest(int expiresIn) {}
    private record SignResponse(String signedURL) {}
}
