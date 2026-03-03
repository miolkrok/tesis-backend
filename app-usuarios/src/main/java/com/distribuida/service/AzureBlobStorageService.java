package com.distribuida.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.PublicAccessType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.UUID;

@Named("azure")
@ApplicationScoped
public class AzureBlobStorageService {


    private final BlobContainerClient containerClient;
    private final String containerName;

    public AzureBlobStorageService(
            @ConfigProperty(name = "azure.storage.connection-string")
            String connectionString,
            @ConfigProperty(name = "azure.storage.container-name")
            String containerName) {

        this.containerName = containerName;

        // Crear cliente del servicio Blob
        BlobServiceClient serviceClient =
                new BlobServiceClientBuilder()
                        .connectionString(connectionString)
                        .buildClient();

        // Obtener referencia al container
        this.containerClient = serviceClient
                .getBlobContainerClient(containerName);

        // Crear container si no existe (primera ejecución)
        if (!containerClient.exists()) {
            containerClient.createWithResponse(
                    null, PublicAccessType.BLOB, null, null);
            System.out.println(
                    "Container creado: " + containerName);
        }

        System.out.println(
                "AzureBlobStorageService inicializado"
                        + " - Container: " + containerName);
    }

    // ==========================================

    // MÉTODOS PÚBLICOS
    // ==========================================

    /**
     * Sube una imagen desde Base64 y retorna la URL pública.
     *
     * @param base64Image    imagen en Base64 desde la app móvil
     * @param folder         carpeta (ej: "actividades/5")
     * @param originalFileName nombre original del archivo
     * @return URL pública del blob en Azure
     */
    public String uploadImageFromBase64(
            String base64Image,
            String folder,
            String originalFileName) {

        try {
            // 1. Limpiar Base64 (quitar prefijo data:image/...)
            String cleanBase64 = base64Image;
            if (base64Image.contains(",")) {
                cleanBase64 = base64Image.split(",")[1];
            }

            // 2. Decodificar Base64 a bytes puros
            byte[] imageBytes = Base64.getDecoder()
                    .decode(cleanBase64);

            // 3. Validar tamaño (máximo 10MB)
            if (imageBytes.length > 10 * 1024 * 1024) {
                throw new IllegalArgumentException(
                        "La imagen excede el tamaño máximo de 10MB");
            }

            // 4. Generar nombre único para el blob
            String blobName = generateBlobName(
                    folder, originalFileName);

            // 5. Detectar content type
            String contentType = detectContentType(
                    base64Image, originalFileName);

            // 6. Obtener referencia al blob
            BlobClient blobClient = containerClient
                    .getBlobClient(blobName);

            // 7. Subir la imagen a Azure Blob Storage
            blobClient.upload(
                    new ByteArrayInputStream(imageBytes),
                    imageBytes.length,
                    true  // overwrite si ya existe
            );

            // 8. Establecer content type HTTP
            blobClient.setHttpHeaders(
                    new BlobHttpHeaders()
                            .setContentType(contentType));

            // 9. Obtener y retornar URL pública
            String publicUrl = blobClient.getBlobUrl();
            System.out.println(
                    "Imagen subida a Azure: " + publicUrl);

            return publicUrl;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            System.err.println(
                    "Error al subir imagen a Azure: "
                            + e.getMessage());
            throw new RuntimeException(
                    "Error al subir imagen: "
                            + e.getMessage(), e);
        }
    }

    /**
     * Elimina una imagen de Azure usando su URL.
     *
     * @param imageUrl URL completa del blob a eliminar
     */
    public void deleteImageByUrl(String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) {
                return;
            }

            String blobName = extractBlobName(imageUrl);
            BlobClient blobClient = containerClient
                    .getBlobClient(blobName);

            if (blobClient.exists()) {
                blobClient.delete();
                System.out.println(
                        "Imagen eliminada: " + blobName);
            }

        } catch (Exception e) {
            System.err.println(
                    "Error al eliminar imagen: "
                            + e.getMessage());
            // No lanzar excepción para no bloquear
            // otras operaciones
        }
    }

    /**
     * Valida que la imagen Base64 sea válida y no exceda
     * el tamaño máximo.
     *
     * @param base64Image imagen en Base64 a validar
     */
    public void validateImage(String base64Image) {
        if (base64Image == null || base64Image.isEmpty()) {
            throw new IllegalArgumentException(
                    "La imagen no puede estar vacía");
        }

        String cleanBase64 = base64Image;
        if (base64Image.contains(",")) {
            cleanBase64 = base64Image.split(",")[1];
        }

        try {
            byte[] imageBytes = Base64.getDecoder()
                    .decode(cleanBase64);

            if (imageBytes.length > 10 * 1024 * 1024) {
                throw new IllegalArgumentException(                    "La imagen excede 10MB");
            }

            if (imageBytes.length < 100) {
                throw new IllegalArgumentException(
                        "La imagen es demasiado pequeña"
                                + " o está corrupta");
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Formato de imagen inválido");
        }
    }

    // ==========================================
    // MÉTODOS PRIVADOS AUXILIARES
    // ==========================================

    /**
     * Genera un nombre único para el blob.
     * Formato: folder/uuid.extension
     */
    private String generateBlobName(
            String folder,
            String originalFileName) {

        String extension = ".jpg";
        if (originalFileName != null
                && originalFileName.contains(".")) {
            extension = originalFileName.substring(
                    originalFileName.lastIndexOf("."));
        }
        return String.format("%s/%s%s",
                folder,
                UUID.randomUUID().toString(),
                extension);
    }

    /**
     * Detecta el content type de la imagen.
     */
    private String detectContentType(
            String base64Image,
            String fileName) {

        // Detectar desde prefijo Base64
        if (base64Image.startsWith("data:image/png"))
            return "image/png";
        if (base64Image.startsWith("data:image/jpeg")
                || base64Image.startsWith("data:image/jpg"))
            return "image/jpeg";
        if (base64Image.startsWith("data:image/gif"))
            return "image/gif";
        if (base64Image.startsWith("data:image/webp"))
            return "image/webp";

        // Detectar desde extensión del archivo
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".png"))  return "image/png";
            if (lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg"))
                return "image/jpeg";
            if (lower.endsWith(".gif"))  return "image/gif";
            if (lower.endsWith(".webp")) return "image/webp";
        }

        return "image/jpeg"; // Por defecto
    }

    /**
     * Extrae el nombre del blob desde su URL.
     * URL: http://host/container/folder/file.jpg
     * Retorna: folder/file.jpg
     */
    private String extractBlobName(String url) {
        String marker = "/" + containerName + "/";
        int idx = url.indexOf(marker);
        if (idx >= 0) {
            return url.substring(idx + marker.length());
        }
        return url.substring(url.lastIndexOf("/") + 1);
    }
}




