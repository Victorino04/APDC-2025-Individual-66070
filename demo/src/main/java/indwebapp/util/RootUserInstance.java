package indwebapp.util;

import jakarta.ejb.Singleton;

import com.google.cloud.datastore.*;

@Singleton
public class RootUserInstance {

  private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
  
  private static RootUserInstance instance;

  private RootUserInstance() {
    String username = "root";
    String password = "root@admin_passwordMVIC";
    String email = "root@email.com";
    String phone = "123456789";
    String fullname = "Root User";
    String visibility = "private";
    String cc = "CC";
    String role = "admin";
    String NIF = "NIF";
    String employer = "employer";
    String function = "function";
    String address = "address";
    String employerNIF = "employerNIF";
    String accountState = "active";
    
    Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
    Entity user = datastore.get(userKey);
    if (user == null) {
      user = Entity.newBuilder(userKey)
          .set("username", username)
          .set("password", password)
          .set("email", email)
          .set("phone", phone)
          .set("fullname", fullname)
          .set("visibility", visibility)
          .set("cc", cc)
          .set("role", role)
          .set("NIF", NIF)
          .set("employer", employer)
          .set("function", function)
          .set("address", address)
          .set("employerNIF", employerNIF)
          .set("accountState", accountState)
          .build();
      datastore.put(user);
    }
  }

  public static synchronized RootUserInstance getInstance() {
    if (instance == null) {
      instance = new RootUserInstance();
    }
    return instance;
  }

}
