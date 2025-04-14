package indwebapp.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.*;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.Response;

@Path("/logout")
public class LogoutResource {

  private static final Logger LOGGER = Logger.getLogger(LogoutResource.class.getName());
  private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

  public LogoutResource() {
  }

  @POST
  public Response logout(@HeaderParam("Authorization") String authHeader) {
    LOGGER.fine("Logout attempt");

    
    Key tokenKey = datastore.newKeyFactory().setKind("Session").newKey(authHeader);
    Entity tokenEntity = datastore.get(tokenKey);

    if (tokenEntity == null) {
      return Response.status(Response.Status.UNAUTHORIZED).entity("No session active").build();
    }

    Transaction transaction = datastore.newTransaction();
    try {
      // Delete the token entity to log out the user
      transaction.delete(tokenKey);
      transaction.commit();
    } catch (DatastoreException e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error logging out").build();
    } finally {
      if (transaction.isActive()) {
        transaction.rollback();
      }
    }

    return Response.ok().entity("User logged out successfully").build();
  }

}
