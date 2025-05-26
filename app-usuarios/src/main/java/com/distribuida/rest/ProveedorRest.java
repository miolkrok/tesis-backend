package com.distribuida.rest;


import com.distribuida.db.Proveedor;
import com.distribuida.db.Usuario;
import com.distribuida.repo.ProveedorRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/proveedores")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class ProveedorRest {

    @Inject
    private ProveedorRepository proveedorRepo;

    @GET
    public List<Proveedor> findAll() {
        System.out.println("findAll");
        return proveedorRepo.listAll();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Integer id) {
        System.out.println("findById");
        var op = proveedorRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(op.get()).build();
    }

    @POST
    public Response create(Proveedor proveedor) {
        proveedor.setId(null);
        proveedorRepo.persist(proveedor);
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, Proveedor proveedor) {
        Proveedor obj = proveedorRepo.findById(id);

        obj.setNombreEmpresa(proveedor.getNombreEmpresa());
        obj.setDescripcionEmpresa(proveedor.getDescripcionEmpresa());
        obj.setLogoEmpresa(obj.getLogoEmpresa());
        obj.setMetodoPago(proveedor.getMetodoPago());
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        proveedorRepo.deleteById(id);
        return Response.ok().build();
    }
}
