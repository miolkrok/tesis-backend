package com.distribuida.repo;


import com.distribuida.db.Busqueda;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.hibernate.search.mapper.orm.session.SearchSession;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
@Transactional
public class BusquedaRepository implements PanacheRepositoryBase<Busqueda, Integer> {

    @Inject
    SearchSession searchSession;

    public List<Busqueda> buscar(String textoBusqueda) {
        return searchSession.search(Busqueda.class)
                .where(f -> f.match()
                        .fields("titulo", "descripcion", "categoria", "ubicacion", "nombreProveedor")
                        .matching(textoBusqueda))
                .fetchHits(20);
    }

    public List<Busqueda> buscarPorCategoria(String categoria) {
        return searchSession.search(Busqueda.class)
                .where(f -> f.match()
                        .field("categoria")
                        .matching(categoria))
                .fetchHits(20);
    }

    public List<Busqueda> buscarPorUbicacion(String ubicacion) {
        return searchSession.search(Busqueda.class)
                .where(f -> f.match()
                        .field("ubicacion")
                        .matching(ubicacion))
                .fetchHits(20);
    }

    public List<Busqueda> buscarConFiltros(String textoBusqueda, String categoria,
                                           String ubicacion, BigDecimal precioMin, BigDecimal precioMax) {

        return searchSession.search(Busqueda.class)
                .where(f -> {
                    var bool = f.bool();

                    if (textoBusqueda != null && !textoBusqueda.isEmpty()) {
                        bool.must(f.match()
                                .fields("titulo", "descripcion", "nombreProveedor")
                                .matching(textoBusqueda));
                    }

                    if (categoria != null && !categoria.isEmpty()) {
                        bool.must(f.match()
                                .field("categoria")
                                .matching(categoria));
                    }

                    if (ubicacion != null && !ubicacion.isEmpty()) {
                        bool.must(f.match()
                                .field("ubicacion")
                                .matching(ubicacion));
                    }

                    if (precioMin != null) {
                        bool.must(f.range()
                                .field("precio")
                                .greaterThan(precioMin));
                    }

                    if (precioMax != null) {
                        bool.must(f.range()
                                .field("precio")
                                .lessThan(precioMax));
                    }

                    return bool;
                })
                .fetchHits(20);
    }
}
