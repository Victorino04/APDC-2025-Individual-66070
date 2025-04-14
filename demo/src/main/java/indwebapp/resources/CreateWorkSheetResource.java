package indwebapp.resources;

import java.util.logging.Logger;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import indwebapp.util.WorkSheet;
import indwebapp.util.AuthToken;

import com.google.cloud.datastore.*;

@Path("/createWorksheet")
public class CreateWorkSheetResource {

  private static final Logger logger = Logger.getLogger(CreateWorkSheetResource.class.getName());
  private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

  public CreateWorkSheetResource() {
  } 

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createWorkSheet(@HeaderParam("Authorization") String authToken, WorkSheet worksheet) {

    logger.fine("Creating worksheet attempt: " + authToken);
    if (authToken == null || authToken.isEmpty()) {
      return Response.status(Response.Status.UNAUTHORIZED).entity("No session active").build();
    }
    Key tokenKey = datastore.newKeyFactory().setKind("Session").newKey(authToken);
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
      logger.warning("Invalid token for user: " + token.user);
      return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid token").build();
    }

    Transaction transaction = datastore.newTransaction();
    try {
      Key worksheetKey = datastore.newKeyFactory().setKind("WorkSheet").newKey(worksheet.reference);
      Entity worksheetEntity = transaction.get(worksheetKey);

      if (token.role.equals("backoffice")) {
        Entity.Builder worksheetBuilder = Entity.newBuilder(worksheetKey);
        worksheetBuilder.set("description", worksheet.description)
        .set("target", worksheet.target)
        .set("awardState", worksheet.awardState);

        if (worksheet.awardState.equals("awarded")) {
          Key entityKey = datastore.newKeyFactory().setKind("Entity").newKey(worksheet.entityId);
          Entity entity = transaction.get(entityKey);
          if (entity == null) {
            transaction.rollback();
            logger.warning("Entity not found: " + worksheet.entityId);
            return Response.status(Response.Status.NOT_FOUND).entity("Entity not found").build();
          }
          if (!entity.getString("role").equals("partner")) {
            transaction.rollback();
            logger.warning("Entity " + worksheet.entityId + " is not a partner.");
            return Response.status(Response.Status.FORBIDDEN).entity("Entity is not a partner").build();
          }
          worksheetBuilder.set("awardDate", worksheet.awardDate)
            .set("startDate", worksheet.startDate)
            .set("endDate", worksheet.endDate)
            .set("entityId", worksheet.entityId)
            .set("entityAward", worksheet.entityAward)
            .set("entityNIF", worksheet.entityNIF)
            .set("workState", worksheet.workState)
            .set("observations", worksheet.observations);
        }
        worksheetEntity = worksheetBuilder.build();
      }
      else if (worksheetEntity != null && token.user.equals(worksheetEntity.getString("entityId"))) {
        worksheetEntity = Entity.newBuilder(worksheetEntity)
          .set("workState", worksheet.workState)
          .build();
      } else {
        transaction.rollback();
        logger.warning("User " + token.user + " is not authorized to modify and/or create this worksheet.");
        return Response.status(Response.Status.FORBIDDEN).entity("User not authorized").build();
      }

      transaction.put(worksheetEntity);
      transaction.commit();
      logger.info("Worksheet created successfully: " + worksheet.reference);
      return Response.status(Response.Status.CREATED).entity("Worksheet created successfully").build();

    } catch (DatastoreException e) {
      logger.severe("Error creating worksheet: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error creating worksheet").build();
    } finally {
      if (transaction.isActive()) {
        transaction.rollback();
      }
    }

  }

}
