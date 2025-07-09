package com.distribuida.repo;


import com.distribuida.db.Busqueda;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@Transactional
public class BusquedaRepository implements PanacheRepositoryBase<Busqueda, Integer> {

    /**
     * Búsqueda con filtros básicos (método existente mejorado)
     */
    public List<Busqueda> buscarConFiltros(String textoBusqueda, String categoria,
                                           String ubicacion, BigDecimal precioMin, BigDecimal precioMax) {

        StringBuilder query = new StringBuilder("estadoActividad = 'ACTIVA'");
        Parameters params = Parameters.with("estadoActivo", "ACTIVA");

        if (textoBusqueda != null && !textoBusqueda.trim().isEmpty()) {
            query.append(" AND (LOWER(titulo) LIKE :texto OR LOWER(descripcion) LIKE :texto OR LOWER(nombreProveedor) LIKE :texto)");
            params.and("texto", "%" + textoBusqueda.toLowerCase() + "%");
        }

        if (categoria != null && !categoria.trim().isEmpty()) {
            query.append(" AND LOWER(categoria) = LOWER(:categoria)");
            params.and("categoria", categoria);
        }

        if (ubicacion != null && !ubicacion.trim().isEmpty()) {
            query.append(" AND (LOWER(ubicacion) LIKE :ubicacion OR LOWER(provincia) LIKE :ubicacion OR LOWER(ciudad) LIKE :ubicacion)");
            params.and("ubicacion", "%" + ubicacion.toLowerCase() + "%");
        }

        if (precioMin != null) {
            query.append(" AND precio >= :precioMin");
            params.and("precioMin", precioMin);
        }

        if (precioMax != null) {
            query.append(" AND precio <= :precioMax");
            params.and("precioMax", precioMax);
        }

        query.append(" ORDER BY puntuacionPromedio DESC, numeroReservas DESC");

        return find(query.toString(), params).list();
    }

    /**
     * Búsqueda avanzada con filtros de fecha y capacidad
     */
    public List<Busqueda> buscarConFiltrosAvanzados(
            String ubicacion, LocalDate fechaInicio, LocalDate fechaFin,
            Integer cantidadPersonas, String tipoActividad,
            BigDecimal precioMin, BigDecimal precioMax,
            Double latitud, Double longitud, Double radioKm) {

        StringBuilder query = new StringBuilder("estadoActividad = 'ACTIVA'");
        Parameters params = Parameters.with("estadoActivo", "ACTIVA");

        // Filtro por ubicación
        if (ubicacion != null && !ubicacion.trim().isEmpty()) {
            query.append(" AND (")
                    .append("LOWER(ciudad) LIKE :ubicacion OR ")
                    .append("LOWER(provincia) LIKE :ubicacion OR ")
                    .append("LOWER(ubicacion) LIKE :ubicacion")
                    .append(")");
            params.and("ubicacion", "%" + ubicacion.toLowerCase() + "%");
        }

        // Filtro por proximidad geográfica
        if (latitud != null && longitud != null && radioKm != null) {
            query.append(" AND latitud IS NOT NULL AND longitud IS NOT NULL");
            query.append(" AND (6371 * acos(cos(radians(:lat)) * cos(radians(latitud)) * ");
            query.append("cos(radians(longitud) - radians(:lng)) + sin(radians(:lat)) * ");
            query.append("sin(radians(latitud)))) <= :radio");
            params.and("lat", latitud).and("lng", longitud).and("radio", radioKm);
        }

        // Filtro por disponibilidad de fechas
        if (fechaInicio != null && fechaFin != null) {
            query.append(" AND (fechaInicioDisponible IS NULL OR fechaInicioDisponible <= :fechaInicio)");
            query.append(" AND (fechaFinDisponible IS NULL OR fechaFinDisponible >= :fechaFin)");
            params.and("fechaInicio", fechaInicio).and("fechaFin", fechaFin);
        }

        // Filtro por capacidad
        if (cantidadPersonas != null) {
            query.append(" AND (minimoPersonas IS NULL OR minimoPersonas <= :cantidadPersonas)");
            query.append(" AND (maximoPersonas IS NULL OR maximoPersonas >= :cantidadPersonas)");
            params.and("cantidadPersonas", cantidadPersonas);
        }

        // Filtro por tipo de actividad
        if (tipoActividad != null && !tipoActividad.trim().isEmpty()) {
            query.append(" AND LOWER(tipoActividad) = LOWER(:tipoActividad)");
            params.and("tipoActividad", tipoActividad);
        }

        // Filtros de precio
        if (precioMin != null) {
            query.append(" AND precio >= :precioMin");
            params.and("precioMin", precioMin);
        }

        if (precioMax != null) {
            query.append(" AND precio <= :precioMax");
            params.and("precioMax", precioMax);
        }

        // Ordenamiento
        if (latitud != null && longitud != null) {
            query.append(" ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(latitud)) * ");
            query.append("cos(radians(longitud) - radians(:lng)) + sin(radians(:lat)) * ");
            query.append("sin(radians(latitud)))) ASC");
        } else {
            query.append(" ORDER BY puntuacionPromedio DESC, numeroReservas DESC");
        }

        return find(query.toString(), params).list();
    }

    /**
     * Búsqueda por proximidad geográfica
     */
    public List<Busqueda> buscarPorProximidad(Double latitud, Double longitud,
                                              Double radioKm, Integer limite) {
        if (latitud == null || longitud == null) {
            return List.of();
        }

        String query = """
            estadoActividad = 'ACTIVA' AND 
            latitud IS NOT NULL AND longitud IS NOT NULL AND
            (6371 * acos(cos(radians(?1)) * cos(radians(latitud)) * 
            cos(radians(longitud) - radians(?2)) + sin(radians(?1)) * 
            sin(radians(latitud)))) <= ?3
            ORDER BY (6371 * acos(cos(radians(?1)) * cos(radians(latitud)) * 
            cos(radians(longitud) - radians(?2)) + sin(radians(?1)) * 
            sin(radians(latitud)))) ASC
            """;

        return find(query, latitud, longitud, radioKm != null ? radioKm : 50.0)
                .page(0, limite != null ? limite : 20)
                .list();
    }

    /**
     * Búsqueda de texto completo mejorada
     */
    public List<Busqueda> buscar(String textoBusqueda) {
        if (textoBusqueda == null || textoBusqueda.trim().isEmpty()) {
            return find("estadoActividad = 'ACTIVA' ORDER BY puntuacionPromedio DESC, numeroReservas DESC")
                    .page(0, 50).list();
        }

        String searchTerm = "%" + textoBusqueda.toLowerCase() + "%";

        return find("""
            estadoActividad = 'ACTIVA' AND (
                LOWER(titulo) LIKE ?1 OR 
                LOWER(descripcion) LIKE ?1 OR 
                LOWER(ubicacion) LIKE ?1 OR
                LOWER(ciudad) LIKE ?1 OR
                LOWER(provincia) LIKE ?1 OR
                LOWER(tipoActividad) LIKE ?1 OR
                LOWER(nombreProveedor) LIKE ?1
            ) ORDER BY 
                CASE 
                    WHEN LOWER(titulo) LIKE ?1 THEN 1
                    WHEN LOWER(tipoActividad) LIKE ?1 THEN 2
                    WHEN LOWER(ciudad) LIKE ?1 THEN 3
                    ELSE 4
                END,
                puntuacionPromedio DESC,
                numeroReservas DESC
            """, searchTerm).list();
    }

    /**
     * Obtener actividades populares
     */
    public List<Busqueda> obtenerActividadesPopulares(Integer limite) {
        return find("estadoActividad = 'ACTIVA' ORDER BY numeroReservas DESC, puntuacionPromedio DESC")
                .page(0, limite != null ? limite : 10)
                .list();
    }

    /**
     * Obtener actividades mejor valoradas
     */
    public List<Busqueda> obtenerActividadesMejorValoradas(Integer limite) {
        return find("estadoActividad = 'ACTIVA' AND puntuacionPromedio > 0 ORDER BY puntuacionPromedio DESC, numeroOpiniones DESC")
                .page(0, limite != null ? limite : 10)
                .list();
    }

    /**
     * Búsqueda por categoría/tipo de actividad
     */
    public List<Busqueda> buscarPorCategoria(String categoria) {
        if (categoria == null || categoria.trim().isEmpty()) {
            return find("estadoActividad = 'ACTIVA' ORDER BY puntuacionPromedio DESC").list();
        }
        return find("estadoActividad = 'ACTIVA' AND LOWER(categoria) = LOWER(?1) ORDER BY puntuacionPromedio DESC", categoria).list();
    }

    /**
     * Búsqueda por ubicación
     */
    public List<Busqueda> buscarPorUbicacion(String ubicacion) {
        if (ubicacion == null || ubicacion.trim().isEmpty()) {
            return find("estadoActividad = 'ACTIVA' ORDER BY puntuacionPromedio DESC").list();
        }
        String searchTerm = "%" + ubicacion.toLowerCase() + "%";
        return find("""
            estadoActividad = 'ACTIVA' AND (
                LOWER(ubicacion) LIKE ?1 OR 
                LOWER(provincia) LIKE ?1 OR 
                LOWER(ciudad) LIKE ?1
            ) ORDER BY puntuacionPromedio DESC
            """, searchTerm).list();
    }

    // ===== MÉTODOS PARA FILTROS Y METADATOS =====

    public List<String> obtenerProvinciasDisponibles() {
        return find("SELECT DISTINCT provincia FROM Busqueda WHERE provincia IS NOT NULL AND estadoActividad = 'ACTIVA' ORDER BY provincia")
                .project(String.class)
                .list();
    }

    public List<String> obtenerTiposActividadDisponibles() {
        return find("SELECT DISTINCT tipoActividad FROM Busqueda WHERE tipoActividad IS NOT NULL AND estadoActividad = 'ACTIVA' ORDER BY tipoActividad")
                .project(String.class)
                .list();
    }

    public List<String> obtenerNivelesDificultadDisponibles() {
        return find("SELECT DISTINCT nivelDificultad FROM Busqueda WHERE nivelDificultad IS NOT NULL AND estadoActividad = 'ACTIVA' ORDER BY nivelDificultad")
                .project(String.class)
                .list();
    }

    public List<String> obtenerCiudadesDisponibles() {
        return find("SELECT DISTINCT ciudad FROM Busqueda WHERE ciudad IS NOT NULL AND estadoActividad = 'ACTIVA' ORDER BY ciudad")
                .project(String.class)
                .list();
    }

    public Map<String, Object> obtenerRangosPrecios() {
        Object[] result = find("SELECT MIN(precio), MAX(precio), AVG(precio) FROM Busqueda WHERE estadoActividad = 'ACTIVA' AND precio IS NOT NULL")
                .project(Object[].class)
                .firstResult();

        return Map.of(
                "minimo", result != null && result[0] != null ? result[0] : 0,
                "maximo", result != null && result[1] != null ? result[1] : 0,
                "promedio", result != null && result[2] != null ? result[2] : 0
        );
    }

    public Map<String, Object> obtenerEstadisticasGenerales() {
        long total = count("estadoActividad = 'ACTIVA'");
        long conOpiniones = count("estadoActividad = 'ACTIVA' AND numeroOpiniones > 0");
        long populares = count("estadoActividad = 'ACTIVA' AND numeroReservas > 5");

        return Map.of(
                "totalActividades", total,
                "actividadesConOpiniones", conOpiniones,
                "actividadesPopulares", populares,
                "porcentajeConOpiniones", total > 0 ? (double) conOpiniones / total * 100 : 0
        );
    }

    /**
     * Obtener sugerencias de autocompletado
     */
    public List<String> obtenerSugerenciasAutocompletado(String texto, String campo) {
        if (texto == null || texto.trim().length() < 2) {
            return List.of();
        }

        String searchTerm = "%" + texto.toLowerCase() + "%";

        return switch (campo.toLowerCase()) {
            case "titulo" -> find("SELECT DISTINCT titulo FROM Busqueda WHERE LOWER(titulo) LIKE ?1 AND estadoActividad = 'ACTIVA' ORDER BY titulo", searchTerm)
                    .project(String.class).page(0, 5).list();
            case "ubicacion" -> find("SELECT DISTINCT ubicacion FROM Busqueda WHERE LOWER(ubicacion) LIKE ?1 AND estadoActividad = 'ACTIVA' ORDER BY ubicacion", searchTerm)
                    .project(String.class).page(0, 5).list();
            case "ciudad" -> find("SELECT DISTINCT ciudad FROM Busqueda WHERE LOWER(ciudad) LIKE ?1 AND estadoActividad = 'ACTIVA' ORDER BY ciudad", searchTerm)
                    .project(String.class).page(0, 5).list();
            case "provincia" -> find("SELECT DISTINCT provincia FROM Busqueda WHERE LOWER(provincia) LIKE ?1 AND estadoActividad = 'ACTIVA' ORDER BY provincia", searchTerm)
                    .project(String.class).page(0, 5).list();
            default -> List.of();
        };
    }
}
