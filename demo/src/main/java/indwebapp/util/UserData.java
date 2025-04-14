package indwebapp.util;

public class UserData {

  public String username;
  public String password;
  public String email;
  public String phone;
  public String fullname;
  public String visibility;
  public String cc;
  public String role;
  public String NIF;
  public String employer;
  public String function;
  public String address;
  public String employerNIF;
  public String accountState;

  public UserData() {
  }

  public UserData(String email, String username, String fullname, String phone, 
                  String password, String visibility, 
                  String cc, String role, String NIF, 
                  String employer, String function, 
                  String address, String employerNIF,
                  String accountState) {
    this.username = username;
    this.password = password;
    this.email = email;
    this.phone = phone;
    this.fullname = fullname;
    this.visibility = visibility;
    this.cc = cc;
    this.role = role;
    this.NIF = NIF;
    this.employer = employer;
    this.function = function;
    this.address = address;
    this.employerNIF = employerNIF;
    this.accountState = accountState;
  }
  
}
