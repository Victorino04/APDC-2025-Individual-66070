package indwebapp.util;

public class ChangeAttributesData extends UserData {

  public String userId;
  
  public ChangeAttributesData() {
  }
  public ChangeAttributesData(String userId, UserData userData) {
    super(userData.email, userData.username, userData.fullname, userData.phone, userData.password, 
    userData.visibility, userData.cc, userData.role, userData.NIF, userData.employer, userData.function, 
    userData.address, userData.employerNIF, userData.accountState);
    this.userId = userId;
  }

}
