package com.distribuida.repo;

import com.distribuida.db.Pago;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class PagoRepository implements PanacheRepositoryBase<Pago, Integer> {

}
