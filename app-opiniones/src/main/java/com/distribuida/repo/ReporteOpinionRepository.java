package com.distribuida.repo;

import com.distribuida.db.ReporteOpinion;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class ReporteOpinionRepository implements PanacheRepositoryBase<ReporteOpinion, Integer> {

}
