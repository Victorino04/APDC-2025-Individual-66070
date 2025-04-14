package indwebapp.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;

import indwebapp.util.AuthToken;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/removeUser")
public class RemoveUserResource {

  private static final Logger LOGGER = Logger.getLogger(RemoveUserResource.class.getName());
  private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
  private final Gson g = new Gson();

  public RemoveUserResource() {
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response removeUser(@HeaderParam("Authorization") String authToken, String username) {
    LOGGER.fine("Remove user attempt with username: " + username);

    if (authToken == null || authToken.isEmpty()) {
      return Response.status(Status.UNAUTHORIZED).entity("No session active").build();
    }
    Key tokenKey = datastore.newKeyFactory().setKind("Session").newKey(authToken);
    Entity tokenEntity = datastore.get(tokenKey);
    if (tokenEntity == null) {
      return Response.status(Status.UNAUTHORIZED).entity("Invalid token").build();
    }
    AuthToken token = new AuthToken(
        tokenEntity.getString("user"),
        tokenEntity.getString("role"),
        tokenEntity.getString("tokenId"),
        tokenEntity.getLong("creationData"),
        tokenEntity.getLong("expirationData")
    );
    if (!token.isValid()) {
      // Token is expired
      Transaction transaction = datastore.newTransaction();
      try {
        // Delete the token entity to log out the user
        transaction.delete(tokenKey);
        transaction.commit();
      } catch (DatastoreException e) {
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error logging out").build();
      } finally {
        if (transaction.isActive()) {
          transaction.rollback();
        }
      }
      return Response.status(Status.UNAUTHORIZED).entity("Token expired").build();
    }
    if (username == null || username.isEmpty()) {
      return Response.status(Status.BAD_REQUEST).entity("Username is required").build();
    }
    if (token.role.equals("enduser") || token.role.equals("partner")) {
      return Response.status(Status.FORBIDDEN).entity("Unauthorized to remove user").build();
    }
    if (username.equals(token.user)) {
      return Response.status(Status.FORBIDDEN).entity("Cannot remove self").build();
    }

    Transaction transaction = datastore.newTransaction();

    try {
      Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
      Entity user = transaction.get(userKey);

      if (user == null) {
        transaction.rollback();
        return Response.status(Status.NOT_FOUND).entity("User not found").build();
      }

      if (!token.role.equals("admin") || !(token.role.equals("backoffice") && 
                                          (user.getString("role").equals("enduser") || user.getString("role").equals("partner")))) {
        transaction.rollback();
        return Response.status(Status.FORBIDDEN).entity("Unauthorized to remove user").build();
      }

      transaction.delete(userKey);
      transaction.commit();

      Transaction tokenTXN = datastore.newTransaction();
      try {
        Query<Entity> query = Query.newEntityQueryBuilder()
            .setKind("Session")
            .setFilter(StructuredQuery.PropertyFilter.eq("user", username))
            .build();
        QueryResults<Entity> results = datastore.run(query);
        while (results.hasNext()) {
          Entity session = results.next();
          Key sessionKey = datastore.newKeyFactory().setKind("Session").newKey(session.getKey().getId());
          tokenTXN.delete(sessionKey);
        }
        tokenTXN.commit();
      } catch (Exception e) {
        tokenTXN.rollback();
        LOGGER.severe("Error removing user token: " + e.getMessage());
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error removing user token").build();
      } finally {
        if (tokenTXN.isActive()) {
          tokenTXN.rollback();
        }
      }

      return Response.ok().entity("User removed successfully").build();

    } catch (Exception e) {
      transaction.rollback();
      LOGGER.severe("Error removing user: " + e.getMessage());
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error removing user").build();
    } finally {
      if (transaction.isActive()) {
        transaction.rollback();
      }
    }
  }

}
