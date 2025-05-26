package com.distribuida.repo;

import com.distribuida.db.Actividad;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class ActividadRepository implements PanacheRepositoryBase<Actividad, Integer> {
}
