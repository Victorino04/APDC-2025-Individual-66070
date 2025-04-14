package indwebapp.util;

public class WorkSheet {

  public String reference;
  public String description;
  public String target;
  public String awardState;
  public String awardDate;
  public String startDate;
  public String endDate;
  public String entityId;
  public String entityAward;
  public String entityNIF;
  public String workState;
  public String observations;

  public WorkSheet() {
  }
  public WorkSheet(String reference, String description, String target, String awardState, 
                String awardDate, String startDate, String endDate, String entityId, String entityAward,
                String entityNIF, String workState, String observations) {
    this.reference = reference;
    this.description = description;
    this.target = target;
    this.awardState = awardState;
    this.awardDate = awardDate;
    this.startDate = startDate;
    this.endDate = endDate;
    this.entityId = entityId;
    this.entityAward = entityAward;
    this.entityNIF = entityNIF;
    this.workState = workState;
    this.observations = observations;
  }

  public boolean isValid() {
    return reference != null && !reference.isEmpty() &&
           description != null && !description.isEmpty() &&
           target != null && !target.isEmpty() &&
           awardState != null && !awardState.isEmpty();
  }

}
