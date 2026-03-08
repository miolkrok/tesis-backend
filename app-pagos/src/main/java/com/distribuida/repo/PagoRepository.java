package com.distribuida.repo;


import com.distribuida.db.Pago;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class PagoRepository implements PanacheRepositoryBase<Pago, Integer> {

    // Método optimizado para obtener pagos por IDs de reserva
    public List<Pago> findPagosByReservaIds(List<Integer> reservaIds) {
        if (reservaIds == null || reservaIds.isEmpty()) {
            return List.of();
        }
        return find("reservaId in ?1", reservaIds).list();
    }

    // Método para obtener pagos de un anfitrión (consulta nativa más eficiente)
    public List<Pago> findPagosByAnfitrionId(Integer anfitrionId) {
        return find("""
            SELECT p FROM Pago p 
            WHERE p.reservaId IN (
                SELECT r.id FROM Reserva r 
                WHERE r.actividadId IN (
                    SELECT a.id FROM Actividad a 
                    WHERE a.usuarioId = ?1
                )
            )
            ORDER BY p.fechaTransaccion DESC
            """, anfitrionId).list();
    }

}
