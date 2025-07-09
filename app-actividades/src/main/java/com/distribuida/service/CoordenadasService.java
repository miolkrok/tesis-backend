package com.distribuida.service;

import lombok.Data;

import java.util.List;
import java.util.Map;

public class CoordenadasService {

    /**
     * Convierte una dirección en coordenadas (simulado - integrar con Google Maps API)
     */
    public Coordenadas obtenerCoordenadas(String direccion) {
        // Simulación - en producción integrar con Google Maps Geocoding API

        // Coordenadas aproximadas de ciudades principales de Ecuador
        Map<String, Coordenadas> ciudadesEcuador = Map.of(
                "quito", new Coordenadas(-0.2298500, -78.5249500, "Quito, Pichincha"),
                "guayaquil", new Coordenadas(-2.1709979, -79.9223592, "Guayaquil, Guayas"),
                "cuenca", new Coordenadas(-2.9001285, -79.0058965, "Cuenca, Azuay"),
                "manta", new Coordenadas(-0.9677429, -80.7749117, "Manta, Manabí"),
                "baños", new Coordenadas(-1.3928, -78.4269, "Baños, Tungurahua"),
                "mindo", new Coordenadas(0.0519, -78.7764, "Mindo, Pichincha"),
                "otavalo", new Coordenadas(0.2341344, -78.2609149, "Otavalo, Imbabura"),
                "puerto lopez", new Coordenadas(-1.5500000, -80.8166700, "Puerto López, Manabí"),
                "montañita", new Coordenadas(-1.8276581, -80.7593016, "Montañita, Santa Elena"),
                "riobamba", new Coordenadas(-1.6635508, -78.6516317, "Riobamba, Chimborazo")
        );

        String direccionLower = direccion.toLowerCase();

        for (Map.Entry<String, Coordenadas> entry : ciudadesEcuador.entrySet()) {
            if (direccionLower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Si no encuentra coincidencia, devolver coordenadas de Quito por defecto
        return new Coordenadas(-0.2298500, -78.5249500, "Ecuador");
    }

    /**
     * Calcula la distancia entre dos puntos usando la fórmula de Haversine
     */
    public double calcularDistancia(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371; // Radio de la Tierra en km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Obtiene sugerencias de ubicaciones basadas en texto
     */
    public List<UbicacionSugerencia> obtenerSugerenciasUbicacion(String texto) {
        // Simulación - en producción usar Google Places API

        if (texto == null || texto.trim().length() < 2) {
            return List.of();
        }

        String textoLower = texto.toLowerCase();

        List<UbicacionSugerencia> todasLasUbicaciones = List.of(
                new UbicacionSugerencia("Quito", "Pichincha", "Ciudad", -0.2298500, -78.5249500),
                new UbicacionSugerencia("Guayaquil", "Guayas", "Ciudad", -2.1709979, -79.9223592),
                new UbicacionSugerencia("Cuenca", "Azuay", "Ciudad", -2.9001285, -79.0058965),
                new UbicacionSugerencia("Baños de Agua Santa", "Tungurahua", "Ciudad", -1.3928, -78.4269),
                new UbicacionSugerencia("Mindo", "Pichincha", "Pueblo", 0.0519, -78.7764),
                new UbicacionSugerencia("Otavalo", "Imbabura", "Ciudad", 0.2341344, -78.2609149),
                new UbicacionSugerencia("Puerto López", "Manabí", "Puerto", -1.5500000, -80.8166700),
                new UbicacionSugerencia("Montañita", "Santa Elena", "Playa", -1.8276581, -80.7593016),
                new UbicacionSugerencia("Riobamba", "Chimborazo", "Ciudad", -1.6635508, -78.6516317),
                new UbicacionSugerencia("Tena", "Napo", "Ciudad", -1.0158479, -77.8109309),
                new UbicacionSugerencia("Puyo", "Pastaza", "Ciudad", -1.4889584, -77.9960026),
                new UbicacionSugerencia("Coca", "Orellana", "Ciudad", -0.4685732, -76.9961525),
                new UbicacionSugerencia("Galápagos", "Galápagos", "Archipiélago", -0.7430608, -90.3021925),
                new UbicacionSugerencia("Volcán Chimborazo", "Chimborazo", "Volcán", -1.4686, -78.8176),
                new UbicacionSugerencia("Volcán Cotopaxi", "Cotopaxi", "Volcán", -0.6854654, -78.4374173)
        );

        return todasLasUbicaciones.stream()
                .filter(ubicacion ->
                        ubicacion.getNombre().toLowerCase().contains(textoLower) ||
                                ubicacion.getProvincia().toLowerCase().contains(textoLower))
                .limit(5)
                .toList();
    }

    /**
     * Determina la provincia basada en coordenadas (simplificado)
     */
    public String determinarProvincia(double latitud, double longitud) {
        // Lógica simplificada - en producción usar reverse geocoding

        if (latitud >= -0.5 && latitud <= 1.0 && longitud >= -79.0 && longitud <= -77.0) {
            return "Pichincha";
        } else if (latitud >= -2.5 && latitud <= -1.5 && longitud >= -80.5 && longitud <= -79.0) {
            return "Guayas";
        } else if (latitud >= -3.2 && latitud <= -2.5 && longitud >= -79.5 && longitud <= -78.5) {
            return "Azuay";
        } else if (latitud >= -1.8 && latitud <= -1.0 && longitud >= -78.8 && longitud <= -78.0) {
            return "Tungurahua";
        }

        return "Ecuador"; // Provincia por defecto
    }

    @Data
    public static class Coordenadas {
        private final double latitud;
        private final double longitud;
        private final String direccionCompleta;

        public Coordenadas(double latitud, double longitud, String direccionCompleta) {
            this.latitud = latitud;
            this.longitud = longitud;
            this.direccionCompleta = direccionCompleta;
        }
    }

    @Data
    public static class UbicacionSugerencia {
        private final String nombre;
        private final String provincia;
        private final String tipo;
        private final double latitud;
        private final double longitud;

        public UbicacionSugerencia(String nombre, String provincia, String tipo,
                                   double latitud, double longitud) {
            this.nombre = nombre;
            this.provincia = provincia;
            this.tipo = tipo;
            this.latitud = latitud;
            this.longitud = longitud;
        }
    }
}
