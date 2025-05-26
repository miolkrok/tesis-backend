package com.distribuida.repo;

import com.distribuida.db.Reserva;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class ReservaRepository implements PanacheRepositoryBase<Reserva,Integer> {
}
