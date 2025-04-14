package indwebapp.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.*;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.gson.Gson;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;

import indwebapp.util.AuthToken;
import jakarta.ws.rs.HeaderParam;

@Path("/listUsers")
@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
public class ListUsers {

  private static final Logger LOGGER = Logger.getLogger(ListUsers.class.getName());
  private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
  private final Gson g = new Gson();

  public ListUsers() {
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response listUsers(@HeaderParam("Authorization") String authHeader) throws IOException {
    LOGGER.fine("List users attempt");

    if (authHeader == null || authHeader.isEmpty()) {
      return Response.status(Response.Status.UNAUTHORIZED).entity("No session active").build();
    }

    Key tokenKey = datastore.newKeyFactory().setKind("Session").newKey(authHeader);
    Entity tokenEntity = datastore.get(tokenKey);

    if (tokenEntity == null) {
      return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid token").build();
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
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error logging out").build();
      } finally {
        if (transaction.isActive()) {
          transaction.rollback();
        }
      }
      return Response.status(Response.Status.UNAUTHORIZED).entity("Token expired").build();
    }

    EntityQuery.Builder queryBuilder = Query.newEntityQueryBuilder()
        .setKind("User");

    switch(tokenEntity.getString("role")) {
      case "admin":
        // Admin can list all users
        break;
      case "backoffice":
        queryBuilder.setFilter(StructuredQuery.PropertyFilter.eq("role", "enduser"));
        break;
      case "enduser":
        queryBuilder
            .setFilter(StructuredQuery.CompositeFilter.and(
          StructuredQuery.PropertyFilter.eq("role", "enduser"),
          StructuredQuery.PropertyFilter.eq("visibility", "public")
            ));
        break;
      default:
        return Response.status(Response.Status.FORBIDDEN).entity("Unauthorized to list users").build();
    }

    Query<Entity> query = queryBuilder.build();

    QueryResults<Entity> results = datastore.run(query);
    StringBuilder userList = new StringBuilder();

    try (CloudTasksClient client = CloudTasksClient.create()) {

      

      if (tokenEntity.getString("role").equals("enduser")) {
        while (results.hasNext()) {
          Entity user = results.next();
          appendUserDetails(userList, user, "username");
          appendUserDetails(userList, user, "email");
          appendUserDetails(userList, user, "phone");
          userList.append("\n");
        }
      }
      else {
        while (results.hasNext()) {
          Entity user = results.next();
          appendUserDetails(userList, user, "username");
          appendUserDetails(userList, user, "email");
          appendUserDetails(userList, user, "phone");
          appendUserDetails(userList, user, "fullname");
          appendUserDetails(userList, user, "visibility");
          appendUserDetails(userList, user, "cc");
          appendUserDetails(userList, user, "role");
          appendUserDetails(userList, user, "NIF");
          appendUserDetails(userList, user, "employer");
          appendUserDetails(userList, user, "function");
          appendUserDetails(userList, user, "address");
          appendUserDetails(userList, user, "employerNIF");
          if (results.hasNext()) {
            appendUserDetails(userList, user, "accountState");
            userList.append("\n");
          }
          else {
            userList.append("accountState: ").append(user.getString("accountState"));
          }
        }
      }
    }

    return Response.ok(userList.toString()).build();
  }

  private void appendUserDetails(StringBuilder userList, Entity User, String attribute) {
    userList.append(String.format("%s: %s\n", attribute, User.getString(attribute)));
  }

}
