package com.distribuida.repo;

import com.distribuida.db.Galeria;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class GaleriaRepository implements PanacheRepositoryBase<Galeria,Integer> {
}
