package indwebapp.resources;

import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;


import com.google.cloud.datastore.*;
import com.google.gson.Gson;

import indwebapp.util.AuthToken;
import indwebapp.util.UserData;
import indwebapp.util.ChangeRoleData;
import indwebapp.util.ChangeAccountStateData;
import indwebapp.util.ChangePasswordData;
import indwebapp.util.ChangeAttributesData;

@Path("/change")
public class ChangePropertiesResource {

  private static final String ILLEGAL_CHANGE_STATE = "You are not allowed to change this user account state: ";
  private static final String ILLEGAL_CHANGE_ROLE = "You are not allowed to change this user role: ";
  private static final String ILLEGAL_CHANGE_OF_STATE = "You are not allowed to change to this account state: ";
  private static final String ILLEGAL_CHANGE_OF_ROLE = "You are not allowed to change to this role: ";

  private static final String USER_NOT_FOUND = "User to change not found";

  private static final Logger LOGGER = Logger.getLogger(ChangePropertiesResource.class.getName());

  private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
  private final Gson g = new Gson();

  public ChangePropertiesResource() {
  }

  @POST
  @Path("/role")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response changeRole(@HeaderParam("Authorization") String authToken, ChangeRoleData data) {
    LOGGER.fine("Change role attempt whit identifier: " + authToken);
    
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
      LOGGER.warning("Invalid token for user: " + token.user);
      return Response.status(Status.UNAUTHORIZED).entity("Invalid token").build();
    }
    if (data.username == null || data.username.isEmpty()) {
      return Response.status(Status.BAD_REQUEST).entity("Username is required").build();
    }
    if (data.role == null || data.role.isEmpty()) {
      return Response.status(Status.BAD_REQUEST).entity("Role is required").build();
    }

    Transaction transaction = datastore.newTransaction();
    try {

      Key userKeyToChange = datastore.newKeyFactory().setKind("User").newKey(data.username);
      Entity userToChange = transaction.get(userKeyToChange);

      if (userToChange == null) {
        transaction.rollback();
        return Response.status(Status.NOT_FOUND).entity(USER_NOT_FOUND).build();
      }

      switch(token.role) {
        case "admin":
          userToChange = Entity.newBuilder(userToChange)
              .set("role", data.role)
              .build();
          transaction.put(userToChange);
          transaction.commit();
          break;
        case "backoffice":
          String role = userToChange.getString("role");
          if(!backOfficeChangeRole(role)) {
            transaction.rollback();
            return Response.status(Status.FORBIDDEN).entity(ILLEGAL_CHANGE_ROLE + role).build();
          }
          if(!backOfficeChangeRole(data.role)) {
            transaction.rollback();
            return Response.status(Status.FORBIDDEN).entity(ILLEGAL_CHANGE_OF_ROLE + data.role).build();
          }
          userToChange = Entity.newBuilder(userToChange)
              .set("role", data.role)
              .build();
          transaction.put(userToChange);
          transaction.commit();
          break;
        default:
          transaction.rollback();
          return Response.status(Status.FORBIDDEN).entity(ILLEGAL_CHANGE_ROLE + token.role).build();

      }
      LOGGER.info("Role changed for user: " + data.username);
      return Response.ok().build();

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

  @POST
  @Path("/accountState")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response changeAccountState(@HeaderParam("Authorization") String authToken, ChangeAccountStateData data) {

    LOGGER.fine("Change account state attempt whit identifier: " + authToken);

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
      LOGGER.warning("Invalid token for user: " + token.user);
      return Response.status(Status.UNAUTHORIZED).entity("Invalid token").build();
    }
    if (data.username == null || data.username.isEmpty()) {
      return Response.status(Status.BAD_REQUEST).entity("Username is required").build();
    }

    Transaction transaction = datastore.newTransaction();
    try {

      Key userKeyToChange = datastore.newKeyFactory().setKind("User").newKey(data.username);
      Entity userToChange = transaction.get(userKeyToChange);

      if (userToChange == null) {
        transaction.rollback();
        return Response.status(Status.NOT_FOUND).entity(USER_NOT_FOUND).build();
      }

      switch(token.role) {
        case "admin":
          userToChange = Entity.newBuilder(userToChange)
              .set("accountState", data.state)
              .build();
          transaction.put(userToChange);
          transaction.commit();
          break;
        case "backoffice":
          String accountState = userToChange.getString("accountState");
          if(!backOfficeChangeAccountState(accountState)) {
            transaction.rollback();
            return Response.status(Status.FORBIDDEN).entity(ILLEGAL_CHANGE_STATE + accountState).build();
          }
          if(!backOfficeChangeAccountState(data.state)) {
            transaction.rollback();
            return Response.status(Status.FORBIDDEN).entity(ILLEGAL_CHANGE_OF_STATE + data.state).build();
          }
          userToChange = Entity.newBuilder(userToChange)
              .set("accountState", data.state)
              .build();
          transaction.put(userToChange);
          transaction.commit();
          break;
        default:
          transaction.rollback();
          return Response.status(Status.FORBIDDEN).entity(ILLEGAL_CHANGE_STATE + token.role).build();

      }
      LOGGER.info("Account state changed for user: " + data.username);
      return Response.ok().build();

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

  @POST
  @Path("/accountAttributes")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response changeAccountAttributes(@HeaderParam("Authorization") String authToken, ChangeAttributesData data) {

    LOGGER.fine("Change account attributes attempt whit identifier: " + authToken);
    
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
      LOGGER.warning("Invalid token for user: " + token.user);
      return Response.status(Status.UNAUTHORIZED).entity("Invalid token").build();
    }
    if (data.username == null || data.username.isEmpty()) {
      return Response.status(Status.BAD_REQUEST).entity("Username is required").build();
    }

    Transaction transaction = datastore.newTransaction();
    try {

      Key userKeyToChange = datastore.newKeyFactory().setKind("User").newKey(data.username);
      Entity userToChange = transaction.get(userKeyToChange);

      if (userToChange == null) {
        transaction.rollback();
        return Response.status(Status.NOT_FOUND).entity(USER_NOT_FOUND).build();
      }

      switch(token.role) {
        case "admin" -> {

            if (!data.username.equals(data.username)) {
              Key newUserKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
              Entity newUser = transaction.get(newUserKey);
  
              if (newUser != null) {
                  transaction.rollback();
                  return Response.status(Status.CONFLICT).entity("Username already exists").build();
              }

              userToChange = Entity.newBuilder(userToChange)
                  .setKey(newUserKey)
                  .set("username", data.username)
                  .build();

              datastore.delete(userKeyToChange);
              
            }

            userToChange = updateUserEntity(userToChange, data)
                .set("username", data.username)
                .set("email", data.email)
                .set("fullname", data.fullname)
                .set("role", data.role)
                .set("accountState", data.accountState)
                .build();
            
            transaction.put(userToChange);
            transaction.commit();
            }
        case "backoffice" -> {
            String email = userToChange.getString("email");
            String username = userToChange.getString("username");
            if(!email.equals(data.email) || !username.equals(data.username)) {
                transaction.rollback();
                return Response.status(Status.FORBIDDEN).entity("You are not allowed to change this user account attributes: email and username").build();
            }
            String accountState = userToChange.getString("accountState");
            if(!backOfficeChangeAccountState(accountState)) {
                transaction.rollback();
                return Response.status(Status.FORBIDDEN).entity(ILLEGAL_CHANGE_STATE + accountState).build();
            }
            if (!backOfficeChangeAccountState(data.accountState)) {
                transaction.rollback();
                return Response.status(Status.FORBIDDEN).entity(ILLEGAL_CHANGE_OF_STATE + data.accountState).build();
            }
            String role = userToChange.getString("role");
            if(!backOfficeChangeRole(role)) {
                transaction.rollback();
                return Response.status(Status.FORBIDDEN).entity(ILLEGAL_CHANGE_ROLE + role).build();
            }
            if (!backOfficeChangeRole(data.role)) {
                transaction.rollback();
                return Response.status(Status.FORBIDDEN).entity(ILLEGAL_CHANGE_OF_ROLE + data.role).build();
            }
            userToChange = updateUserEntity(userToChange, data)
                .set("fullname", data.fullname)
                .set("role", data.role)
                .set("accountState", data.accountState)
                .build();
            transaction.put(userToChange);
            transaction.commit();
            }
        case "enduser" -> {
          if (!data.username.equals(token.user)) {
            transaction.rollback();
            return Response.status(Status.FORBIDDEN).entity("You are not allowed to change this user account attributes: " + token.user).build();
          }
          if (!userToChange.getString("accountState").equals("active")) {
            transaction.rollback();
            return Response.status(Status.FORBIDDEN).entity("You are not allowed to change this user account attributes while disactivated: " + token.user).build();
          }
          userToChange = updateUserEntity(userToChange, data)
              .build();
          transaction.put(userToChange);
          transaction.commit();
        }

      }
      LOGGER.info("Account attributes changed for user: " + data.username);
      return Response.ok().build();

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

  @POST
  @Path("/password")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response changePassword(@HeaderParam("Authorization") String authToken, ChangePasswordData data) {

    LOGGER.fine("Change password attempt whit identifier: " + authToken);
    
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
      LOGGER.warning("Invalid token for user: " + token.user);
      return Response.status(Status.UNAUTHORIZED).entity("Invalid token").build();
    }
    if (data.oldPassword == null || data.oldPassword.isEmpty()) {
      return Response.status(Status.BAD_REQUEST).entity("Password is required").build();
    }
    if (data.newPassword == null || data.newPassword.isEmpty()) {
      return Response.status(Status.BAD_REQUEST).entity("New password is required").build();
    }
    if (data.newPassword2 == null || data.newPassword2.isEmpty()) {
      return Response.status(Status.BAD_REQUEST).entity("New password confirmation is required").build();
    }

    Transaction transaction = datastore.newTransaction();
    try {

      Key userKeyToChange = datastore.newKeyFactory().setKind("User").newKey(token.user);
      Entity userToChange = transaction.get(userKeyToChange);

      String hashedPWD = (String) userToChange.getString("password");
      if (!hashedPWD.equals(DigestUtils.sha512Hex(data.oldPassword))) {
        transaction.rollback();
        LOGGER.warning("Incorrect password for user: " + token.user);
        return Response.status(Status.FORBIDDEN).entity("Incorrect password").build();
      }

      if (!data.newPassword.equals(data.newPassword2)) {
        transaction.rollback();
        LOGGER.warning("New password and confirmation does not match for user: " + token.user);
        return Response.status(Status.FORBIDDEN).entity("Password does not match").build();
      }

      userToChange = Entity.newBuilder(userToChange)
          .set("password", DigestUtils.sha512Hex(data.newPassword))
          .build();

      transaction.put(userToChange);
      transaction.commit();

      LOGGER.info("Password changed for user: " + token.user);
      return Response.ok().build();

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

  private Entity.Builder updateUserEntity(Entity userToChange, UserData data) {
    return Entity.newBuilder(userToChange)
        .set("password", DigestUtils.sha512Hex(data.password))
        .set("phone", data.phone)
        .set("visibility", data.visibility)
        .set("cc", data.cc)
        .set("NIF", data.NIF)
        .set("employer", data.employer)
        .set("function", data.function)
        .set("address", data.address)
        .set("employerNIF", data.employerNIF);
  }

  private boolean backOfficeChangeRole(String role) {
    return role.equals("enduser") || role.equals("partner");
  }
  private boolean backOfficeChangeAccountState(String accountState) {
    return accountState.equals("active") || accountState.equals("inactive");
  }

}
