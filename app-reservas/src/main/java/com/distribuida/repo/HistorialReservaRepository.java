package com.distribuida.repo;

import com.distribuida.db.HistorialReserva;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class HistorialReservaRepository implements PanacheRepositoryBase<HistorialReserva, Integer> {
}
