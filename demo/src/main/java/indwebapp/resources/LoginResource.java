package indwebapp.resources;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import indwebapp.util.AuthToken;
import indwebapp.util.LoginData;

import com.google.cloud.datastore.*;

import com.google.gson.Gson;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
public class LoginResource {

  private static final String INVALID_USER_CREDENTIALS = "Incorrect username or password";

  private static final Logger LOGGER = Logger.getLogger(LoginResource.class.getName());
  private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
  private final Gson g = new Gson();

  public LoginResource() {
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response login(LoginData loginData) {
    if (loginData == null || loginData.identifier == null || loginData.password == null) {
      LOGGER.warning("Login attempt with null identifier or password");
      return Response.status(Status.BAD_REQUEST).entity(INVALID_USER_CREDENTIALS).build();
    }
    LOGGER.log(Level.FINE, "Login attempt with identifier: {0}", loginData.identifier);

    Key userKey = datastore.newKeyFactory().setKind("User").newKey(loginData.identifier);

    Transaction transaction = datastore.newTransaction();

    try {
      Entity user = transaction.get(userKey);

      if (user == null) {
        transaction.rollback();
        LOGGER.warning("User does not exist: " + loginData.identifier);
        return Response.status(Status.FORBIDDEN).entity(INVALID_USER_CREDENTIALS).build();
      }

      String hashedPWD = (String) user.getString("password");
      if (!hashedPWD.equals(DigestUtils.sha512Hex(loginData.password))) {
        transaction.rollback();
        LOGGER.warning("Incorrect password for user: " + loginData.identifier);
        return Response.status(Status.FORBIDDEN).entity(INVALID_USER_CREDENTIALS).build();
      }

      AuthToken authToken = new AuthToken(loginData.identifier, user.getString("role"));

      LOGGER.info("Login successful for user: " + loginData.identifier);

      // Store the token in the datastore
      Entity tokenEntity = Entity.newBuilder(datastore.newKeyFactory().setKind("Session").newKey(authToken.tokenId))
          .set("user", authToken.user)
          .set("role", authToken.role)
          .set("tokenId", authToken.tokenId)
          .set("creationData", authToken.creationData)
          .set("expirationData", authToken.expirationData)
          .build();

      transaction.put(tokenEntity);
      transaction.commit();
      LOGGER.info("Token stored successfully for user: " + loginData.identifier);

      return Response.ok(authToken.tokenId).build();

    } catch (Exception e) {
      transaction.rollback();
      LOGGER.severe(e.getMessage());
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    } finally {
      if (transaction.isActive()) {
        transaction.rollback();
      }
    }
    
  }

}
