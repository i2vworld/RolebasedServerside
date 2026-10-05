package com.world.i2v.i2vauthapi.service;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.ListBlobsOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Uploads files to Azure Blob Storage using a DETERMINISTIC directory so we
 * never create a new random/alphanumeric folder on every upload.
 *
 * Blob path:  {directory}/{originalFilename}   (directory defaults to
 * submissionId/fieldName when no explicit folder is supplied). Re-uploading the
 * same file overwrites it in place.
 */
@Service
public class FileStorageService {

    private final String connectionString;
    private final String container;

    private BlobServiceClient client;

    public FileStorageService(
            @Value("${azure.storage.connection-string:}") String connectionString,
            @Value("${azure.storage.container:i2v-uploads}") String container) {
        this.connectionString = connectionString == null ? "" : connectionString;
        this.container = container;
    }

    private BlobServiceClient client() {
        if (client == null) {
            if (connectionString.isBlank()) {
                throw new IllegalStateException(
                        "azure.storage.connection-string is not configured; cannot store files.");
            }
            client = new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
        }
        return client;
    }

    private String sanitizeDirectory(String directory) {
        if (directory == null || directory.isBlank()) return "";
        return directory.replaceAll("^/+|/+$", "")
                .replaceAll("[^a-zA-Z0-9._/-]", "_");
    }

    private String buildBlobName(String directory, String fileName) {
        String safeDir = sanitizeDirectory(directory);
        String safeFile = fileName == null || fileName.isBlank() ? "file" : fileName;
        return safeDir.isBlank() ? safeFile : safeDir + "/" + safeFile;
    }

    public String upload(String directory, String fileName, MultipartFile file) throws IOException {
        String blobName = buildBlobName(directory, fileName);
        var containerClient = client().getBlobContainerClient(container);
        var blobClient = containerClient.getBlobClient(blobName);
        try (InputStream in = file.getInputStream()) {
            blobClient.upload(in, file.getSize(), true); // overwrite = true
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
        }
        return blobClient.getBlobUrl();
    }

    /** Returns a ZIP of every blob under the given submission directory. */
    public byte[] downloadSubmissionZip(String submissionId) throws IOException {
        String prefix = sanitizeDirectory(submissionId);
        String finalPrefix = prefix.isBlank() ? prefix : prefix + "/";
        var containerClient = client().getBlobContainerClient(container);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            containerClient.listBlobs(new ListBlobsOptions().setPrefix(finalPrefix), null, null)
                    .forEach(blob -> {
                        String name = blob.getName();
                        var blobClient = containerClient.getBlobClient(name);
                        byte[] data = blobClient.downloadContent().toBytes();
                        String entryName = name.substring(prefix.length());
                        String finalEntryName = entryName.isBlank() ? name : entryName;
                        ZipEntry entry = new ZipEntry(finalEntryName);
                        try {
                            zip.putNextEntry(entry);
                            zip.write(data);
                            zip.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        return baos.toByteArray();
    }

    public void deleteByUrl(String blobUrl) {
        if (blobUrl == null || blobUrl.isBlank()) return;
        String blobName = extractBlobName(blobUrl);
        if (blobName == null) return;
        var containerClient = client().getBlobContainerClient(container);
        containerClient.getBlobClient(blobName).deleteIfExists();
    }

    private String extractBlobName(String blobUrl) {
        try {
            URL url = new URL(blobUrl);
            String path = url.getPath();                 // /container/blobName
            int idx = path.indexOf("/" + container + "/");
            if (idx < 0) return null;
            String name = path.substring(idx + container.length() + 2);
            return URLDecoder.decode(name, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
