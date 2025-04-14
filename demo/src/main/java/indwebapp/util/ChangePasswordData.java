package indwebapp.util;

public class ChangePasswordData {

  public String username;
  public String oldPassword;
  public String newPassword;
  public String newPassword2;

  public ChangePasswordData() {
  }

  public ChangePasswordData(String username, String oldPassword, String newPassword, String newPassword2) {
    this.username = username;
    this.oldPassword = oldPassword;
    this.newPassword = newPassword;
    this.newPassword2 = newPassword2;
  }

  public boolean isNewPasswordsEqual() {
    return newPassword.equals(newPassword2);
  }

}
