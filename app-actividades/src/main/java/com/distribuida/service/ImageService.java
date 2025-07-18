package com.distribuida.service;

import com.distribuida.db.Galeria;
import com.distribuida.dtos.GaleriaDTO;
import com.distribuida.repo.GaleriaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class ImageService {

    @Inject
    GaleriaRepository galeriaRepository;

    public GaleriaDTO convertToDTO(Galeria galeria) {
        GaleriaDTO dto = new GaleriaDTO();
        dto.setId(galeria.getId());
        dto.setUrlFoto(galeria.getUrlFoto());
        dto.setActividadId(galeria.getActividad() != null ? galeria.getActividad().getId() : null);
        dto.setNombreArchivo(galeria.getNombreArchivo());
        dto.setTipoContenido(galeria.getTipoContenido());
        dto.setTamanoArchivo(galeria.getTamanoArchivo());
        dto.setEsImagenPrincipal(galeria.getEsImagenPrincipal());

        // Convertir imagen binaria a Base64 si existe
        if (galeria.getImagenBinaria() != null) {
            String base64Image = Base64.getEncoder().encodeToString(galeria.getImagenBinaria());
            dto.setImagenBase64(base64Image);
        }

        return dto;
    }

    public Galeria convertToEntity(GaleriaDTO dto) {
        Galeria galeria = new Galeria();
        galeria.setId(dto.getId());
        galeria.setUrlFoto(dto.getUrlFoto());
        galeria.setNombreArchivo(dto.getNombreArchivo());
        galeria.setTipoContenido(dto.getTipoContenido());
        galeria.setTamanoArchivo(dto.getTamanoArchivo());
        galeria.setEsImagenPrincipal(dto.getEsImagenPrincipal());

        // Convertir imagen Base64 a binario si existe
        if (dto.getImagenBase64() != null && !dto.getImagenBase64().isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(dto.getImagenBase64());
                galeria.setImagenBinaria(imageBytes);
            } catch (Exception e) {
                System.err.println("Error al decodificar imagen Base64: " + e.getMessage());
            }
        }

        return galeria;
    }

    public List<GaleriaDTO> obtenerImagenesPorActividad(Integer actividadId) {
        List<Galeria> imagenes = galeriaRepository.list("actividad.id", actividadId);
        return imagenes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public GaleriaDTO obtenerImagenPrincipal(Integer actividadId) {
        Galeria imagenPrincipal = galeriaRepository.find(
                "actividad.id = ?1 AND esImagenPrincipal = true",
                actividadId
        ).firstResultOptional().orElse(null);

        if (imagenPrincipal == null) {
            // Si no hay imagen principal, tomar la primera disponible
            imagenPrincipal = galeriaRepository.find(
                    "actividad.id = ?1",
                    actividadId
            ).firstResultOptional().orElse(null);
        }

        return imagenPrincipal != null ? convertToDTO(imagenPrincipal) : null;
    }

}
