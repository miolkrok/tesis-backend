package com.distribuida.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.Base64;
import java.util.UUID;

@ApplicationScoped
public class S3StorageServiceMinio {

    private final S3Client s3Client;
    private final String bucketName;
    private final String endpoint;
    private final boolean usePathStyleAccess;

    public S3StorageServiceMinio(
            @ConfigProperty(name = "aws.s3.bucket-name") String bucketName,
            @ConfigProperty(name = "aws.s3.region") String region,
            @ConfigProperty(name = "aws.s3.access-key") String accessKey,
            @ConfigProperty(name = "aws.s3.secret-key") String secretKey,
            @ConfigProperty(name = "aws.s3.endpoint", defaultValue = "") String endpoint,
            @ConfigProperty(name = "aws.s3.path-style-access", defaultValue = "false") boolean pathStyleAccess) {

        this.bucketName = bucketName;
        this.endpoint = endpoint;
        this.usePathStyleAccess = pathStyleAccess;

        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);

        // 🔥 Configuración que funciona tanto para MinIO como AWS S3
        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccess)
                .build();

        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .serviceConfiguration(s3Config);

        // Si hay endpoint personalizado (MinIO), usarlo
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
            System.out.println("🔧 Usando endpoint personalizado: " + endpoint);
        }

        this.s3Client = builder.build();

        System.out.println("S3StorageService inicializado");
        System.out.println("Bucket: " + bucketName);
        System.out.println("Region: " + region);
        System.out.println("Endpoint: " + (endpoint.isEmpty() ? "AWS S3" : endpoint));
    }

    /**
     * Sube una imagen desde Base64 a S3/MinIO y retorna la URL pública
     */
    public String uploadImageFromBase64(String base64Image, String folder, String originalFileName) {
        try {
            // Limpiar el Base64
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

            // Subir a S3/MinIO
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
            e.printStackTrace();
            throw new RuntimeException("Error al subir imagen: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina una imagen de S3/MinIO usando su URL
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
            System.out.println("Imagen eliminada de S3: " + fileName);

        } catch (Exception e) {
            System.err.println("Error al eliminar imagen de S3: " + e.getMessage());
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
            extension = ".jpg";
        }

        String uniqueId = UUID.randomUUID().toString();
        return String.format("%s/%s%s", folder, uniqueId, extension);
    }

    /**
     * Construye la URL pública del objeto
     */
    private String getPublicUrl(String fileName) {
        if (!endpoint.isEmpty()) {
            // MinIO: usar endpoint personalizado
            return String.format("%s/%s/%s", endpoint, bucketName, fileName);
        } else {
            // AWS S3: URL estándar
            return String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileName);
        }
    }

    /**
     * Extrae el nombre del archivo desde la URL
     */
    private String extractFileNameFromUrl(String url) {
        // Para MinIO: http://localhost:9000/bucket/folder/file.jpg
        // Para AWS: https://bucket.s3.amazonaws.com/folder/file.jpg

        if (url.contains("/" + bucketName + "/")) {
            return url.substring(url.lastIndexOf("/" + bucketName + "/") + bucketName.length() + 2);
        }

        if (url.contains(".com/")) {
            return url.substring(url.lastIndexOf(".com/") + 5);
        }

        return url.substring(url.lastIndexOf("/") + 1);
    }

    /**
     * Detecta el tipo de contenido de la imagen
     */
    private String detectContentType(String base64Image, String fileName) {
        if (base64Image.startsWith("data:image/png")) {
            return "image/png";
        } else if (base64Image.startsWith("data:image/jpeg") || base64Image.startsWith("data:image/jpg")) {
            return "image/jpeg";
        } else if (base64Image.startsWith("data:image/gif")) {
            return "image/gif";
        } else if (base64Image.startsWith("data:image/webp")) {
            return "image/webp";
        }

        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            if (lowerFileName.endsWith(".png")) return "image/png";
            if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) return "image/jpeg";
            if (lowerFileName.endsWith(".gif")) return "image/gif";
            if (lowerFileName.endsWith(".webp")) return "image/webp";
        }

        return "image/jpeg";
    }

    /**
     * Valida que la imagen sea válida
     */
    public void validateImage(String base64Image) {
        if (base64Image == null || base64Image.isEmpty()) {
            throw new IllegalArgumentException("La imagen no puede estar vacía");
        }

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
