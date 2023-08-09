package com.example.javarestappjaxrs;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerResource {
    private static final List<Customers> customers = new ArrayList<>();

    static {
        // Adding sample customer data
        customers.add(new Customers(1, "Alice"));
        customers.add(new Customers(2, "Bob"));
    }


    @GET
    public List<Customers> getAllCustomers() {
        return customers;
    }

    @POST
    public Response addCustomer(Customers customer) {
        customer.setId(generateCustomerId());
        customers.add(customer);
        return Response.status(Response.Status.CREATED).entity(customer).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateCustomer(@PathParam("id") int id, Customers updatedCustomer) {
        Customers existingCustomer = findCustomerById(id);
        if (existingCustomer != null) {
            existingCustomer.setName(updatedCustomer.getName());

            return Response.ok(existingCustomer).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
    private Customers findCustomerById(int id) {
        for (Customers customer : customers) {
            if (customer.getId() == id) {
                return customer;
            }
        }
        return null;
    }

    @DELETE
    @Path("/{id}")
    public Response deleteCustomer(@PathParam("id") int id) {
        Customers customer = findCustomerById(id);
        if (customer != null) {
            customers.remove(customer);
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
    private int generateCustomerId() {
        return customers.size() + 1;
    }

}
