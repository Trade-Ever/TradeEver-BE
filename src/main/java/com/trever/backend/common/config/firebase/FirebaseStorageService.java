package com.trever.backend.common.config.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.cloud.StorageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FirebaseStorageService {

    @Value("${firebase.storage.bucket}")
    private String bucketName;

    @Value("${firebase.config.path}")
    private String firebaseConfigPath;

    /**
     * 이미지 파일을 Firebase Storage에 업로드하고 다운로드 URL을 반환합니다.
     *
     * @param file        업로드할 이미지 파일
     * @param folderName  저장할 폴더명 (예: "vehicles")
     * @return 업로드된 파일의 다운로드 URL
     * @throws IOException 파일 처리 중 예외 발생 시
     */
    public String uploadImage(MultipartFile file, String folderName) throws IOException {
        // 파일 유효성 검사
        if (file.isEmpty()) {
            throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
        }

        // 파일 확장자 확인
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(fileName);

        if (!isImageFile(extension)) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }

        // Storage 인스턴스 얻기
        Storage storage = StorageOptions.newBuilder()
                .setCredentials(GoogleCredentials.fromStream(
                        new ClassPathResource(firebaseConfigPath).getInputStream()))
                .build()
                .getService();

        // 고유한 파일명 생성
        String uniqueFileName = UUID.randomUUID().toString() + "." + extension;

        // 저장 경로 설정
        String objectName = folderName + "/" + uniqueFileName;

        // BlobId 및 BlobInfo 생성
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        // 파일 업로드
        Blob blob = storage.create(blobInfo, file.getBytes());

        // 다운로드 URL 생성 (1년 유효)
        String downloadUrl = blob.signUrl(365, TimeUnit.DAYS).toString();

        log.info("File uploaded successfully: {}", downloadUrl);

        return downloadUrl;
    }

    /**
     * Firebase Storage에서 파일 삭제
     *
     * @param fileUrl 삭제할 파일의 URL
     * @return 삭제 성공 여부
     */
    public boolean deleteImage(String fileUrl) {
        try {
            // URL에서 객체 경로 추출
            String objectName = extractObjectNameFromUrl(fileUrl);

            if (objectName == null) {
                return false;
            }

            // Storage 인스턴스 얻기
            Storage storage = StorageOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(
                            new java.io.FileInputStream(firebaseConfigPath)))
                    .build()
                    .getService();

            // 파일 삭제
            BlobId blobId = BlobId.of(bucketName, objectName);
            boolean deleted = storage.delete(blobId);

            if (deleted) {
                log.info("File deleted successfully: {}", objectName);
            } else {
                log.warn("File could not be deleted: {}", objectName);
            }

            return deleted;
        } catch (Exception e) {
            log.error("Error deleting file: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 파일 확장자가 이미지 파일인지 확인
     */
    private boolean isImageFile(String extension) {
        if (extension == null) {
            return false;
        }

        String ext = extension.toLowerCase();
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") ||
               ext.equals("gif") || ext.equals("bmp") || ext.equals("webp");
    }

    /**
     * Firebase Storage URL에서 객체 경로 추출
     */
    private String extractObjectNameFromUrl(String fileUrl) {
        // Firebase Storage URL 형식:
        // https://firebasestorage.googleapis.com/v0/b/BUCKET_NAME/o/ENCODED_OBJECT_PATH?token=...
        try {
            String urlWithoutParams = fileUrl.split("\\?")[0];
            String encodedPath = urlWithoutParams.split("/o/")[1];
            return java.net.URLDecoder.decode(encodedPath, "UTF-8");
        } catch (Exception e) {
            log.error("Error extracting object name from URL: {}", e.getMessage(), e);
            return null;
        }
    }
}