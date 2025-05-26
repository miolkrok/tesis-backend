package com.distribuida.repo;


import com.distribuida.db.Proveedor;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class ProveedorRepository implements PanacheRepositoryBase<Proveedor, Integer> {
}
