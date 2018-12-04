package eu.arrowhead.client.provider;

import eu.arrowhead.client.common.Utility;
import eu.arrowhead.client.common.exception.ArrowheadException;
import eu.arrowhead.client.common.exception.AuthException;
import eu.arrowhead.client.common.misc.SecurityUtils;
import eu.arrowhead.client.common.model.RawTokenInfo;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.security.Signature;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.ws.rs.core.SecurityContext;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

class RequestVerification {

  /*
    Based on the local Authorization public key and provider private key, this method verifies that the provided token/signature pair
    was created by the Authorization Core System with the provider public key. It also checks if the token expired or not, plus the token
    has to contain the same consumer name as the common name field of the client certificate.
   */
  static void verifyRequester(SecurityContext context, String token, String signature) {
    try {
      String commonName = SecurityUtils.getCertCNFromSubject(context.getUserPrincipal().getName());
      String[] commonNameParts = commonName.split("\\.");
      String consumerName = commonNameParts[0];

      /*System.out.println(token);
      System.out.println(signature);*/
      if (token.contains(" ")) {
        token = token.replaceAll("\\s", "+");
      }
      if (signature.contains(" ")) {
        signature = signature.replaceAll("\\s", "+");
      }

      byte[] tokenbytes = Base64.getDecoder().decode(token);
      byte[] signaturebytes = Base64.getDecoder().decode(signature);

      Security.addProvider(new BouncyCastleProvider());
      Signature signatureInstance = Signature.getInstance("SHA256withRSA", "BC");
      signatureInstance.initVerify(FullProviderMain.authorizationKey);
      signatureInstance.update(tokenbytes);

      boolean verifies = signatureInstance.verify(signaturebytes);
      if (!verifies) {
        throw new AuthException("Authorization core system signature verification failed!");
      }

      Cipher cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding", "BC");
      cipher.init(Cipher.DECRYPT_MODE, FullProviderMain.privateKey);
      //Check if the provider public key registered in the database is the same as the one used by the provider at the moment
      byte[] byteToken = cipher.doFinal(tokenbytes);

      String json = new String(byteToken, StandardCharsets.UTF_8);
      RawTokenInfo rawTokenInfo = Utility.fromJson(json, RawTokenInfo.class);
      String[] rawTokenInfoParts = rawTokenInfo.getC().split("\\.");
      String consumerTokenName = rawTokenInfoParts[0];

      long endTime = rawTokenInfo.getE();
      long currentTime = System.currentTimeMillis();

      if (consumerName.equalsIgnoreCase(consumerTokenName)) {
        if (endTime == 0L || (endTime > currentTime)) {
          return;
        }
        throw new AuthException("Given token has expired!");

      } else {
        throw new AuthException("Cert common name and token information are mismatched!");
      }

    } catch (Exception ex) {
      throw new ArrowheadException("Internal Server Error during token validation!", 500, ex);
    }
  }
}
