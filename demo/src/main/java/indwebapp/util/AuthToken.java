package indwebapp.util;

import java.util.UUID;


public class AuthToken {
  
  public static final long EXPIRATION_TIME = 1000*60*60*2;

  public String user;
  public String role;
  public String tokenId;
  public long creationData;
  public long expirationData;

  
  public AuthToken(){
  }

  public AuthToken(String identifier, String role) {
    this.user = identifier;
    this.role = role;
    this.tokenId = UUID.randomUUID().toString();
    this.creationData = System.currentTimeMillis();
    this.expirationData = creationData - EXPIRATION_TIME;
  }
  public AuthToken(String identifier,String role, String tokenId, long creationData, long expirationData) {
    this.user = identifier;
    this.role = role;
    this.tokenId = tokenId;
    this.creationData = creationData;
    this.expirationData = expirationData;
  }

  public boolean isValid() {
    return (System.currentTimeMillis() > expirationData);
  }

}
