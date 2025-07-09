package com.distribuida.service;

import com.distribuida.db.Actividad;
import com.distribuida.repo.ActividadRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class DisponibilidadService {

    @Inject
    ActividadRepository actividadRepository;

    /**
     * Verifica si una actividad está disponible para las fechas y cantidad solicitadas
     */
    public boolean verificarDisponibilidad(Integer actividadId, LocalDate fechaInicio,
                                           LocalDate fechaFin, Integer cantidadPersonas) {

        Actividad actividad = actividadRepository.findById(actividadId);
        if (actividad == null || !"ACTIVA".equals(actividad.getEstadoActividad())) {
            return false;
        }

        // Verificar rango de fechas disponibles
        if (actividad.getFechaInicioDisponible() != null &&
                fechaInicio.isBefore(actividad.getFechaInicioDisponible())) {
            return false;
        }

        if (actividad.getFechaFinDisponible() != null &&
                fechaFin.isAfter(actividad.getFechaFinDisponible())) {
            return false;
        }

        // Verificar capacidad mínima y máxima
        if (actividad.getMinimoPersonas() != null &&
                cantidadPersonas < actividad.getMinimoPersonas()) {
            return false;
        }

        if (actividad.getMaximoPersonas() != null &&
                cantidadPersonas > actividad.getMaximoPersonas()) {
            return false;
        }

        // Aquí podrías agregar lógica más compleja:
        // - Verificar reservas existentes para esas fechas
        // - Verificar disponibilidad específica por día
        // - Verificar bloqueos del proveedor

        return true;
    }

    /**
     * Obtiene las fechas disponibles para una actividad en un rango
     */
    public List<LocalDate> obtenerFechasDisponibles(Integer actividadId,
                                                    LocalDate fechaInicio,
                                                    LocalDate fechaFin) {

        Actividad actividad = actividadRepository.findById(actividadId);
        if (actividad == null) {
            return List.of();
        }

        LocalDate inicio = fechaInicio;
        LocalDate fin = fechaFin;

        // Ajustar al rango disponible de la actividad
        if (actividad.getFechaInicioDisponible() != null &&
                inicio.isBefore(actividad.getFechaInicioDisponible())) {
            inicio = actividad.getFechaInicioDisponible();
        }

        if (actividad.getFechaFinDisponible() != null &&
                fin.isAfter(actividad.getFechaFinDisponible())) {
            fin = actividad.getFechaFinDisponible();
        }

        return inicio.datesUntil(fin.plusDays(1))
                .collect(Collectors.toList());
    }

    /**
     * Calcula el precio para una fecha específica
     */
    public Double calcularPrecio(Integer actividadId, LocalDate fecha, Integer cantidadPersonas) {

        Actividad actividad = actividadRepository.findById(actividadId);
        if (actividad == null || actividad.getPrecio() == null) {
            return 0.0;
        }

        double precioBase = actividad.getPrecio().doubleValue();

        // Aplicar modificadores de precio (ejemplos):

        // Descuento por grupo grande
        if (cantidadPersonas >= 5) {
            precioBase *= 0.9; // 10% descuento
        }

        // Precio especial en temporada alta (ejemplo: diciembre-enero)
        if (fecha.getMonthValue() == 12 || fecha.getMonthValue() == 1) {
            precioBase *= 1.2; // 20% incremento
        }

        // Descuento en días de semana
        if (fecha.getDayOfWeek().getValue() >= 1 && fecha.getDayOfWeek().getValue() <= 5) {
            precioBase *= 0.95; // 5% descuento
        }

        return precioBase * cantidadPersonas;
    }

    /**
     * Obtiene estadísticas de disponibilidad para dashboard del proveedor
     */
    public Map<String, Object> obtenerEstadisticasDisponibilidad(Integer proveedorId,
                                                                 LocalDate fechaInicio,
                                                                 LocalDate fechaFin) {

        List<Actividad> actividades = actividadRepository.find("usuarioId", proveedorId).list();

        long actividadesActivas = actividades.stream()
                .filter(a -> "ACTIVA".equals(a.getEstadoActividad()))
                .count();

        long actividadesDisponibles = actividades.stream()
                .filter(a -> "ACTIVA".equals(a.getEstadoActividad()))
                .filter(a -> verificarDisponibilidadBasica(a, fechaInicio, fechaFin))
                .count();

        return Map.of(
                "totalActividades", actividades.size(),
                "actividadesActivas", actividadesActivas,
                "actividadesDisponibles", actividadesDisponibles,
                "porcentajeDisponibilidad", actividadesActivas > 0 ?
                        (double) actividadesDisponibles / actividadesActivas * 100 : 0
        );
    }

    private boolean verificarDisponibilidadBasica(Actividad actividad,
                                                  LocalDate fechaInicio,
                                                  LocalDate fechaFin) {
        return (actividad.getFechaInicioDisponible() == null ||
                !fechaInicio.isBefore(actividad.getFechaInicioDisponible())) &&
                (actividad.getFechaFinDisponible() == null ||
                        !fechaFin.isAfter(actividad.getFechaFinDisponible()));
    }

    /**
     * Bloquea fechas específicas para una actividad
     */
    public void bloquearFechas(Integer actividadId, List<LocalDate> fechas, String motivo) {
        // Implementar lógica de bloqueo
        // Esto podría usar una tabla separada de "bloqueos" o "disponibilidad_específica"
        System.out.println("Bloqueando fechas " + fechas + " para actividad " + actividadId +
                " - Motivo: " + motivo);
    }

    /**
     * Libera fechas bloqueadas
     */
    public void liberarFechas(Integer actividadId, List<LocalDate> fechas) {
        // Implementar lógica de liberación
        System.out.println("Liberando fechas " + fechas + " para actividad " + actividadId);
    }
}
