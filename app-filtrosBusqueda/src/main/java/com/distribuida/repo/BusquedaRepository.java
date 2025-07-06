package com.distribuida.repo;


import com.distribuida.db.Busqueda;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
@Transactional
public class BusquedaRepository implements PanacheRepositoryBase<Busqueda, Integer> {

    public List<Busqueda> buscar(String textoBusqueda) {
        if (textoBusqueda == null || textoBusqueda.trim().isEmpty()) {
            return listAll();
        }

        String searchTerm = "%" + textoBusqueda.toLowerCase() + "%";

        return find("LOWER(titulo) LIKE ?1 OR " +
                        "LOWER(descripcion) LIKE ?1 OR " +
                        "LOWER(categoria) LIKE ?1 OR " +
                        "LOWER(ubicacion) LIKE ?1 OR " +
                        "LOWER(nombreProveedor) LIKE ?1",
                searchTerm).list();
    }

    public List<Busqueda> buscarPorCategoria(String categoria) {
        if (categoria == null || categoria.trim().isEmpty()) {
            return listAll();
        }
        return find("LOWER(categoria) = LOWER(?1)", categoria).list();
    }

    public List<Busqueda> buscarPorUbicacion(String ubicacion) {
        if (ubicacion == null || ubicacion.trim().isEmpty()) {
            return listAll();
        }
        return find("LOWER(ubicacion) = LOWER(?1)", ubicacion).list();
    }

    public List<Busqueda> buscarConFiltros(String textoBusqueda, String categoria,
                                           String ubicacion, BigDecimal precioMin, BigDecimal precioMax) {

        StringBuilder query = new StringBuilder("1=1");

        if (textoBusqueda != null && !textoBusqueda.trim().isEmpty()) {
            query.append(" AND (LOWER(titulo) LIKE ?1 OR LOWER(descripcion) LIKE ?1 OR LOWER(nombreProveedor) LIKE ?1)");
        }

        if (categoria != null && !categoria.trim().isEmpty()) {
            query.append(" AND LOWER(categoria) = LOWER(?").append(getNextParamIndex(textoBusqueda, 2)).append(")");
        }

        if (ubicacion != null && !ubicacion.trim().isEmpty()) {
            query.append(" AND LOWER(ubicacion) = LOWER(?").append(getNextParamIndex(textoBusqueda, categoria, 3)).append(")");
        }

        if (precioMin != null) {
            query.append(" AND precio >= ?").append(getNextParamIndex(textoBusqueda, categoria, ubicacion, 4));
        }

        if (precioMax != null) {
            query.append(" AND precio <= ?").append(getNextParamIndex(textoBusqueda, categoria, ubicacion, precioMin, 5));
        }

        // Construir parámetros dinámicamente
        Object[] params = buildParams(textoBusqueda, categoria, ubicacion, precioMin, precioMax);

        return find(query.toString(), params).list();
    }

    private int getNextParamIndex(Object... previousParams) {
        int index = 1;
        for (Object param : previousParams) {
            if (param != null && (!(param instanceof String) || !((String) param).trim().isEmpty())) {
                index++;
            }
        }
        return index;
    }

    private Object[] buildParams(String textoBusqueda, String categoria, String ubicacion,
                                 BigDecimal precioMin, BigDecimal precioMax) {
        java.util.ArrayList<Object> paramList = new java.util.ArrayList<>();

        if (textoBusqueda != null && !textoBusqueda.trim().isEmpty()) {
            paramList.add("%" + textoBusqueda.toLowerCase() + "%");
        }

        if (categoria != null && !categoria.trim().isEmpty()) {
            paramList.add(categoria);
        }

        if (ubicacion != null && !ubicacion.trim().isEmpty()) {
            paramList.add(ubicacion);
        }

        if (precioMin != null) {
            paramList.add(precioMin);
        }

        if (precioMax != null) {
            paramList.add(precioMax);
        }

        return paramList.toArray();
    }
}
