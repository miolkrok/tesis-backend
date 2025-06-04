package com.distribuida.repo;

import com.distribuida.db.ServicioEvento;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class ServicioEventoRepository implements PanacheRepositoryBase<ServicioEvento,Integer> {

}
