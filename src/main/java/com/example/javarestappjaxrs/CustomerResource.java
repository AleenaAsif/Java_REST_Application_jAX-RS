package com.example.javarestappjaxrs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.core.HttpHeaders;


@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerResource {
    private static final Logger logger = LoggerFactory.getLogger("FILE");

    private static final String DB_URL = "jdbc:mysql://localhost:3306/restappdata";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "root";




    @GET
    public Response getAllCustomers(@HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader) {
        logger.info("GET request received");
        logger.info("API Request: GET /customers");
        try {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                logger.error("Error loading JDBC driver");
                e.printStackTrace();
            }

            // Check if the Authorization header is provided
            if (authHeader == null || !authHeader.toLowerCase().startsWith("basic ")) {
            // Unauthorized access: Return 401 Unauthorized status
            return Response.status(Response.Status.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Realm Name\"")
                    .build();
        }

        // Decode and parse the username and password from the Authorization header
        String credentials = authHeader.substring("basic ".length()).trim();
        byte[] decodedCredentials = Base64.getDecoder().decode(credentials);
        String decodedString = new String(decodedCredentials, StandardCharsets.UTF_8);
        String[] usernameAndPassword = decodedString.split(":", 2);
        String username = usernameAndPassword[0];
        String password = usernameAndPassword[1];

            // Check if the username and password match the credentials from the database
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {

                statement.setString(1, username);
                statement.setString(2, password);
                ResultSet resultSet = statement.executeQuery();
                logger.info("customers sent");


                if (!resultSet.next()) {
                    logger.warn ("Unauthorized access: Invalid credentials for user {}",username);
                    // Unauthorized access: Prompt the user to enter correct credentials
                    return Response.status(Response.Status.UNAUTHORIZED)
                            .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Realm Name\"")
                            .entity("Invalid credentials. Please enter your correct username and password.")
                            .build(); //
                }

            } catch (SQLException e) {
                logger.error("Error validating credentials", e);
                e.printStackTrace();
                return Response.serverError().build();
            }

            // Valid credentials: Proceed with fetching customers
            List<Customers> customers = new ArrayList<>();
            Connection connection = null;
            PreparedStatement statement = null;
            ResultSet resultSet = null;

            try {
                // Load the MySQL JDBC driver
                Class.forName("com.mysql.cj.jdbc.Driver");

                // Establish a database connection
                connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);

                // Create a prepared statement for the SELECT query
                statement = connection.prepareStatement("SELECT * FROM customers");

                // Execute the query and retrieve the result set
                resultSet = statement.executeQuery();

                // Iterate through the result set and populate the customers list
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String name = resultSet.getString("name");
                    customers.add(new Customers(id, name));
                }

            } catch (ClassNotFoundException | SQLException e) {
                logger.error("Error fetching customers", e);
                e.printStackTrace();
            } finally {
                // Close the resources in a finally block to ensure they are closed
                try {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                    if (statement != null) {
                        statement.close();
                    }
                    if (connection != null) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            return Response.ok(customers).build();
        } catch (Exception e) {
            logger.error("Error closing resources", e);
            e.printStackTrace();
            return Response.serverError().build();
        }
    }
    @POST
    public Response addCustomer(Customers customer) {
        logger.info("API Request: POST /customers");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("Error loading JDBC driver", e);
            e.printStackTrace();
        }
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement("INSERT INTO customers (name) VALUES (?) ",
                     Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, customer.getName());
            statement.executeUpdate();

            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                int generatedId = generatedKeys.getInt(1);
                customer.setId(generatedId); // Set the generated ID in the Customers object
            }
            logger.info("Added customer: {}", customer);

            return Response.status(Response.Status.CREATED).entity(customer).build();

        } catch (SQLException e) {
            logger.error("Error adding customer", e);
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateCustomer(@PathParam("id") int id, Customers updatedCustomer) {
        logger.info("API Request: PUT /customers/{}", id);
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement("UPDATE customers SET name = ? WHERE id = ?")) {

            statement.setString(1, updatedCustomer.getName());
            statement.setInt(2, id);
            int updatedRowCount = statement.executeUpdate();
            if (updatedRowCount > 0) {
                logger.info("Updated customer: {}", updatedCustomer);
                return Response.ok(updatedCustomer).build();
            } else {
                logger.warn("Customer not found with ID: {}", id);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (SQLException e) {
            logger.error("Error updating customer", e);
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteCustomer(@PathParam("id") int id) {
        logger.info("API Request: DELETE /customers/{}", id);
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement("DELETE FROM customers WHERE id = ?")) {

            statement.setInt(1, id);
            int deletedRowCount = statement.executeUpdate();
            if (deletedRowCount > 0) {
                logger.info("Deleted customer with ID: {}", id);
                return Response.ok().build();
            } else {
                logger.warn("Customer not found with ID: {}", id);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (SQLException e) {
            logger.error("Error deleting customer", e);
            e.printStackTrace();
            return Response.serverError().build();
        }
    }
}


//EXTRAS

//    private static final String HARD_CODED_USERNAME = "hardcoded_user";
//    private static final String HARD_CODED_PASSWORD = "hardcoded_password";
//    @GET
//    public Response getAllCustomers(@HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader) {
//        // Check if the Authorization header is provided
//        if (authHeader == null || !authHeader.toLowerCase().startsWith("basic ")) {
//            // Unauthorized access: Return 401 Unauthorized status
//            return Response.status(Response.Status.UNAUTHORIZED)
//                    .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Realm Name\"")
//                    .build();
//        }
//
//        // Decode and parse the username and password from the Authorization header
//        String credentials = authHeader.substring("basic ".length()).trim();
//        byte[] decodedCredentials = Base64.getDecoder().decode(credentials);
//        String decodedString = new String(decodedCredentials, StandardCharsets.UTF_8);
//        String[] usernameAndPassword = decodedString.split(":", 2);
//        String username = usernameAndPassword[0];
//        String password = usernameAndPassword[1];
//
//        // Check if the username and password match the hardcoded credentials
//        if (!username.equals(HARD_CODED_USERNAME) || !password.equals(HARD_CODED_PASSWORD)) {
//            // Unauthorized access: Return 401 Unauthorized status
//            return Response.status(Response.Status.UNAUTHORIZED)
//                    .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Realm Name\"")
//                    .build();
//        }
//
//        // Valid credentials: Proceed with fetching customers
//        List<Customers> customers = new ArrayList<>();
//        Connection connection = null;
//        PreparedStatement statement = null;
//        ResultSet resultSet = null;
//
//        try {
//            // Load the MySQL JDBC driver
//            Class.forName("com.mysql.cj.jdbc.Driver");
//
//            // Establish a database connection
//            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
//
//            // Create a prepared statement for the SELECT query
//            statement = connection.prepareStatement("SELECT * FROM customers");
//
//            // Execute the query and retrieve the result set
//            resultSet = statement.executeQuery();
//
//            // Iterate through the result set and populate the customers list
//            while (resultSet.next()) {
//                int id = resultSet.getInt("id");
//                String name = resultSet.getString("name");
//                customers.add(new Customers(id, name));
//            }
//
//        } catch (ClassNotFoundException | SQLException e) {
//            e.printStackTrace();
//        } finally {
//
//            try {
//                if (resultSet != null) {
//                    resultSet.close();
//                }
//                if (statement != null) {
//                    statement.close();
//                }
//                if (connection != null) {
//                    connection.close();
//                }
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }
//
//        return Response.ok(customers).build();
//    }