package com.distribuida.repo;

import com.distribuida.db.Reembolso;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class ReembolsoRepository implements PanacheRepositoryBase<Reembolso, Integer> {

}
