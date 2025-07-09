package com.distribuida.repo;

import com.distribuida.db.Actividad;
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
public class ActividadRepository implements PanacheRepositoryBase<Actividad, Integer> {

    /**
     * Búsqueda principal con ubicación, fechas y capacidad
     */
    public List<Actividad> buscarActividadesDisponibles(
            String ubicacion,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer cantidadPersonas,
            String tipoActividad,
            BigDecimal precioMinimo,
            BigDecimal precioMaximo) {

        StringBuilder query = new StringBuilder();
        Parameters params = Parameters.with("estadoActivo", "ACTIVA");

        query.append("estadoActividad = :estadoActivo");

        // Filtro por ubicación (ciudad, provincia o ubicación destino)
        if (ubicacion != null && !ubicacion.trim().isEmpty()) {
            query.append(" AND (")
                    .append("LOWER(ciudad) LIKE :ubicacion OR ")
                    .append("LOWER(provincia) LIKE :ubicacion OR ")
                    .append("LOWER(ubicacionDestino) LIKE :ubicacion")
                    .append(")");
            params.and("ubicacion", "%" + ubicacion.toLowerCase() + "%");
        }

        // Filtro por disponibilidad de fechas
        if (fechaInicio != null && fechaFin != null) {
            query.append(" AND fechaInicioDisponible <= :fechaInicio")
                    .append(" AND fechaFinDisponible >= :fechaFin");
            params.and("fechaInicio", fechaInicio)
                    .and("fechaFin", fechaFin);
        }

        // Filtro por capacidad de personas
        if (cantidadPersonas != null) {
            query.append(" AND (minimoPersonas IS NULL OR minimoPersonas <= :cantidadPersonas)")
                    .append(" AND (maximoPersonas IS NULL OR maximoPersonas >= :cantidadPersonas)");
            params.and("cantidadPersonas", cantidadPersonas);
        }

        // Filtro por tipo de actividad
        if (tipoActividad != null && !tipoActividad.trim().isEmpty()) {
            query.append(" AND LOWER(tipoActividad) = LOWER(:tipoActividad)");
            params.and("tipoActividad", tipoActividad);
        }

        // Filtro por rango de precios
        if (precioMinimo != null) {
            query.append(" AND precio >= :precioMinimo");
            params.and("precioMinimo", precioMinimo);
        }

        if (precioMaximo != null) {
            query.append(" AND precio <= :precioMaximo");
            params.and("precioMaximo", precioMaximo);
        }

        query.append(" ORDER BY fechaCreacion DESC");

        return find(query.toString(), params).list();
    }

    /**
     * Búsqueda por proximidad geográfica (requiere coordenadas)
     */
    public List<Actividad> buscarPorProximidad(
            Double latitud,
            Double longitud,
            Double radioKm,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer cantidadPersonas) {

        // Fórmula de Haversine simplificada para calcular distancia
        String query = """
            SELECT a FROM Actividad a WHERE 
            a.estadoActividad = 'ACTIVA' AND
            a.latitud IS NOT NULL AND a.longitud IS NOT NULL AND
            (6371 * acos(cos(radians(:lat)) * cos(radians(a.latitud)) * 
            cos(radians(a.longitud) - radians(:lng)) + sin(radians(:lat)) * 
            sin(radians(a.latitud)))) <= :radio
            """;

        Parameters params = Parameters.with("lat", latitud)
                .and("lng", longitud)
                .and("radio", radioKm);

        if (fechaInicio != null && fechaFin != null) {
            query += " AND a.fechaInicioDisponible <= :fechaInicio AND a.fechaFinDisponible >= :fechaFin";
            params.and("fechaInicio", fechaInicio).and("fechaFin", fechaFin);
        }

        if (cantidadPersonas != null) {
            query += " AND (a.minimoPersonas IS NULL OR a.minimoPersonas <= :cantidadPersonas)";
            query += " AND (a.maximoPersonas IS NULL OR a.maximoPersonas >= :cantidadPersonas)";
            params.and("cantidadPersonas", cantidadPersonas);
        }

        query += " ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(a.latitud)) * cos(radians(a.longitud) - radians(:lng)) + sin(radians(:lat)) * sin(radians(a.latitud)))) ASC";

        return find(query, params).list();
    }

    /**
     * Búsqueda rápida por texto
     */
    public List<Actividad> busquedaRapida(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return find("estadoActividad = 'ACTIVA' ORDER BY fechaCreacion DESC").list();
        }

        String searchTerm = "%" + texto.toLowerCase() + "%";

        return find("""
            estadoActividad = 'ACTIVA' AND (
                LOWER(titulo) LIKE ?1 OR 
                LOWER(descripcion) LIKE ?1 OR 
                LOWER(ubicacionDestino) LIKE ?1 OR
                LOWER(ciudad) LIKE ?1 OR
                LOWER(provincia) LIKE ?1 OR
                LOWER(tipoActividad) LIKE ?1
            ) ORDER BY fechaCreacion DESC
            """, searchTerm).list();
    }

    /**
     * Obtener actividades populares (con más reservas)
     */
    public List<Actividad> obtenerActividadesPopulares(int limite) {
        // Esta consulta asumiría que tienes un contador de reservas
        // Por ahora ordenamos por fecha de creación como proxy
        return find("estadoActividad = 'ACTIVA' ORDER BY fechaCreacion DESC")
                .page(0, limite)
                .list();
    }

    /**
     * Filtros adicionales para la interfaz
     */
    public List<String> obtenerProvinciasDisponibles() {
        return find("SELECT DISTINCT provincia FROM Actividad WHERE provincia IS NOT NULL AND estadoActividad = 'ACTIVA' ORDER BY provincia")
                .project(String.class)
                .list();
    }

    public List<String> obtenerTiposActividadDisponibles() {
        return find("SELECT DISTINCT tipoActividad FROM Actividad WHERE tipoActividad IS NOT NULL AND estadoActividad = 'ACTIVA' ORDER BY tipoActividad")
                .project(String.class)
                .list();
    }

    public Map<String, Object> obtenerRangosPrecios() {
        Object[] result = find("SELECT MIN(precio), MAX(precio), AVG(precio) FROM Actividad WHERE estadoActividad = 'ACTIVA' AND precio IS NOT NULL")
                .project(Object[].class)
                .firstResult();

        return Map.of(
                "minimo", result != null ? result[0] : 0,
                "maximo", result != null ? result[1] : 0,
                "promedio", result != null ? result[2] : 0
        );
    }

}
