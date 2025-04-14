package indwebapp.util;

public class RegisterData extends UserData {

  public String confirmation;

  public RegisterData() {
  }

  public RegisterData(String email, String username, String fullname, String phone, 
                      String password, String visibility, 
                      String cc, String role, String NIF, 
                      String employer, String function, 
                      String address, String employerNIF,
                      String accountState,
                      String confirmation) {
    super(email, username, fullname, phone, password, visibility, cc, role, NIF, employer, function, address, employerNIF, accountState);    

    this.confirmation = confirmation;
  }

  private boolean nonEmptyOrBlank(String field) {
    return field != null && !field.isBlank();
  }

  public boolean isValid() {
    return nonEmptyOrBlank(username) &&
           nonEmptyOrBlank(password) &&
           nonEmptyOrBlank(email) &&
           nonEmptyOrBlank(phone) &&
           nonEmptyOrBlank(fullname) &&
           nonEmptyOrBlank(visibility) &&
           email.contains("@");
  }
  public boolean isPasswordValid() {
    return password.equals(confirmation);
  }

}