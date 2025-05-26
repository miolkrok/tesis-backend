package com.distribuida.repo;

import com.distribuida.db.Usuario;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class UsuarioRepository implements PanacheRepositoryBase<Usuario,Integer> {
}
