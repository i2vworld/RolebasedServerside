package com.world.i2v.i2vauthapi.Controller;

import com.world.i2v.i2vauthapi.service.FileStorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Upload one file. Deterministic blob path:
     *   - if `folder` is provided  -> {folder}/{originalFilename}
     *   - else                      -> {submissionId}/{fieldName}/{originalFilename}
     * Re-uploading the same name overwrites it (no new random folder).
     */
    @PostMapping("/upload/{submissionId}")
    public ResponseEntity<Map<String, Object>> upload(
            @PathVariable String submissionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String fieldName,
            @RequestParam(required = false) String folder) {
        try {
            String directory = (folder != null && !folder.isBlank())
                    ? folder
                    : (submissionId + (fieldName != null && !fieldName.isBlank() ? "/" + fieldName : ""));
            return doUpload(directory, file);
        } catch (Exception e) {
            return error(e);
        }
    }

    /** Upload without a submission context (e.g. CV). Uses folder/fieldName only. */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadNoSubmission(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String fieldName,
            @RequestParam(required = false) String folder) {
        try {
            String directory = (folder != null && !folder.isBlank())
                    ? folder
                    : (fieldName != null && !fieldName.isBlank() ? fieldName : "uploads");
            return doUpload(directory, file);
        } catch (Exception e) {
            return error(e);
        }
    }

    /** Download all files for a submission as a ZIP. */
    @GetMapping("/download/{submissionId}")
    public ResponseEntity<byte[]> download(@PathVariable String submissionId) {
        try {
            byte[] zip = fileStorageService.downloadSubmissionZip(submissionId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", submissionId + "-files.zip");
            return new ResponseEntity<>(zip, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /** Delete a previously uploaded blob by its URL. */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> delete(@RequestParam String blobUrl) {
        try {
            fileStorageService.deleteByUrl(blobUrl);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("deleted", true);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return error(e);
        }
    }

    // ==================== HELPERS ====================

    private ResponseEntity<Map<String, Object>> doUpload(String directory, MultipartFile file) throws Exception {
        String original = file.getOriginalFilename();
        String base = (original == null || original.isBlank()) ? "file" : original;
        String name = System.currentTimeMillis() + "_" + base;
        String url = fileStorageService.upload(directory, name, file);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("url", url);
        body.put("fileName", name);
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<Map<String, Object>> error(Exception e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
