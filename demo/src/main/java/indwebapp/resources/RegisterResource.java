package indwebapp.resources;

import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import indwebapp.util.RegisterData;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/register")
@Consumes(MediaType.APPLICATION_JSON)
public class RegisterResource {

  private static final String NOT_DEFINED = "NOT_DEFINED";

  private static final Logger LOGGER = Logger.getLogger(RegisterResource.class.getName());
  private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
  private final Gson g = new Gson();

  public RegisterResource() {
  }

  @POST
  public Response register(RegisterData data) {
    LOGGER.fine("Register attempt with username: " + data.username);
    
    if (!data.isValid()) {
      return Response.status(Status.BAD_REQUEST).entity("Invalid registration data").build();
    }
    if (!data.isPasswordValid()) {
      return Response.status(Status.BAD_REQUEST).entity("Password does not match").build();
    }

    Transaction transaction = datastore.newTransaction();
    
    try {
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Entity user = transaction.get(userKey);

        if (user != null) {
          transaction.rollback();
          return Response.status(Status.CONFLICT).entity("User already exists").build();
        }

        if (data.role.equals("admin") || data.role.equals("enduser")) {
          transaction.rollback();
          return Response.status(Status.FORBIDDEN).entity("Illegal role parameter").build();
        }

        user = Entity.newBuilder(userKey)
            .set("username", data.username)
            .set("password", DigestUtils.sha512Hex(data.password))
            .set("email", data.email)
            .set("phone", data.phone)
            .set("fullname", data.fullname)
            .set("visibility", data.visibility)
            .set("cc", additionalAttributes(data.cc, NOT_DEFINED))
            .set("role", additionalAttributes(data.role, "enduser"))
            .set("NIF", additionalAttributes(data.NIF, NOT_DEFINED))
            .set("employer", additionalAttributes(data.employer, NOT_DEFINED))
            .set("function", additionalAttributes(data.function, NOT_DEFINED))
            .set("address", additionalAttributes(data.address, NOT_DEFINED))
            .set("employerNIF", additionalAttributes(data.employerNIF, NOT_DEFINED))
            .set("accountState", additionalAttributes(data.accountState, "unactive"))
            .build();

        transaction.put(user);
        transaction.commit();

        LOGGER.info("User registered: " + data.username);

        return Response.ok().build();

    } catch (DatastoreException e) {
        return Response.status(Status.INTERNAL_SERVER_ERROR)
            .entity(e.toString()).build();
    } finally {
        if (transaction.isActive()) {
            transaction.rollback();
        }
    }
  }

  private String additionalAttributes(String attribute, String value) {
    return attribute != null && !attribute.isBlank() ? attribute : value;   
  }

}
