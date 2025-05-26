package com.distribuida.repo;

import com.distribuida.db.Opinion;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class OpinionRepository implements PanacheRepositoryBase<Opinion,Integer> {

}
