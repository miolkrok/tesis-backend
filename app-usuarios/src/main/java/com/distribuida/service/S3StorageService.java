package com.distribuida.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Base64;
import java.util.UUID;

@ApplicationScoped
public class S3StorageService {

    private final S3Client s3Client;
    private final String bucketName;

    public S3StorageService(
            @ConfigProperty(name = "aws.s3.bucket-name") String bucketName,
            @ConfigProperty(name = "aws.s3.region") String region,
            @ConfigProperty(name = "aws.s3.access-key") String accessKey,
            @ConfigProperty(name = "aws.s3.secret-key") String secretKey) {

        this.bucketName = bucketName;

        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();

        System.out.println("✅ S3StorageService inicializado - Bucket: " + bucketName);
    }

    /**
     * Sube una imagen desde Base64 a S3 y retorna la URL pública
     */
    public String uploadImageFromBase64(String base64Image, String folder, String originalFileName) {
        try {
            // Limpiar el Base64 (quitar data:image/...;base64,)
            String cleanBase64 = base64Image;
            if (base64Image.contains(",")) {
                cleanBase64 = base64Image.split(",")[1];
            }

            // Decodificar Base64 a bytes
            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);

            // Validar tamaño máximo (10MB)
            if (imageBytes.length > 10 * 1024 * 1024) {
                throw new IllegalArgumentException("La imagen excede el tamaño máximo de 10MB");
            }

            // Generar nombre único para el archivo
            String fileName = generateFileName(folder, originalFileName);

            // Detectar tipo de contenido
            String contentType = detectContentType(base64Image, originalFileName);

            // Subir a S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));

            // Retornar URL pública
            String publicUrl = getPublicUrl(fileName);
            System.out.println("Imagen subida exitosamente: " + publicUrl);

            return publicUrl;

        } catch (Exception e) {
            System.err.println("Error al subir imagen a S3: " + e.getMessage());
            throw new RuntimeException("Error al subir imagen: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina una imagen de S3 usando su URL
     */
    public void deleteImageByUrl(String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) {
                return;
            }

            String fileName = extractFileNameFromUrl(imageUrl);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            System.out.println("🗑️ Imagen eliminada de S3: " + fileName);

        } catch (Exception e) {
            System.err.println("Error al eliminar imagen de S3: " + e.getMessage());
            // No lanzar excepción para no bloquear otras operaciones
        }
    }

    /**
     * Genera un nombre único para el archivo
     */
    private String generateFileName(String folder, String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        } else {
            extension = ".jpg"; // Extensión por defecto
        }

        String uniqueId = UUID.randomUUID().toString();
        return String.format("%s/%s%s", folder, uniqueId, extension);
    }

    /**
     * Construye la URL pública del objeto en S3
     */
    private String getPublicUrl(String fileName) {
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileName);
    }

    /**
     * Extrae el nombre del archivo desde la URL
     */
    private String extractFileNameFromUrl(String url) {
        if (url.contains(".com/")) {
            return url.substring(url.lastIndexOf(".com/") + 5);
        }
        return url.substring(url.lastIndexOf("/") + 1);
    }

    /**
     * Detecta el tipo de contenido de la imagen
     */
    private String detectContentType(String base64Image, String fileName) {
        // Detectar desde el prefijo Base64
        if (base64Image.startsWith("data:image/png")) {
            return "image/png";
        } else if (base64Image.startsWith("data:image/jpeg") || base64Image.startsWith("data:image/jpg")) {
            return "image/jpeg";
        } else if (base64Image.startsWith("data:image/gif")) {
            return "image/gif";
        } else if (base64Image.startsWith("data:image/webp")) {
            return "image/webp";
        }

        // Detectar desde la extensión del archivo
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            if (lowerFileName.endsWith(".png")) return "image/png";
            if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) return "image/jpeg";
            if (lowerFileName.endsWith(".gif")) return "image/gif";
            if (lowerFileName.endsWith(".webp")) return "image/webp";
        }

        // Por defecto
        return "image/jpeg";
    }

    /**
     * Valida que la imagen sea válida
     */
    public void validateImage(String base64Image) {
        if (base64Image == null || base64Image.isEmpty()) {
            throw new IllegalArgumentException("La imagen no puede estar vacía");
        }

        // Limpiar Base64
        String cleanBase64 = base64Image;
        if (base64Image.contains(",")) {
            cleanBase64 = base64Image.split(",")[1];
        }

        try {
            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);

            if (imageBytes.length > 10 * 1024 * 1024) {
                throw new IllegalArgumentException("La imagen excede el tamaño máximo de 10MB");
            }

            if (imageBytes.length < 100) {
                throw new IllegalArgumentException("La imagen es demasiado pequeña o está corrupta");
            }

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Formato de imagen inválido: " + e.getMessage());
        }
    }
}
