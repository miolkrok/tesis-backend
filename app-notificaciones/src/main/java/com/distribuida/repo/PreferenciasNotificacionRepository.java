package com.distribuida.repo;

import com.distribuida.db.PreferenciasNotificacion;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class PreferenciasNotificacionRepository implements PanacheRepositoryBase<PreferenciasNotificacion, Integer> {
}
