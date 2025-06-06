package com.distribuida.repo;

import com.distribuida.db.Notificacion;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class NotificacionRepository implements PanacheRepositoryBase<Notificacion, Integer> {
}
