package eu.arrowhead.client.common.no_need_to_modify;

import eu.arrowhead.client.common.no_need_to_modify.exception.ErrorMessage;
import eu.arrowhead.client.common.no_need_to_modify.exception.ExceptionType;
import eu.arrowhead.client.common.no_need_to_modify.misc.SecurityUtils;
import eu.arrowhead.client.common.no_need_to_modify.model.RawTokenInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Properties;
import java.util.function.Supplier;

public abstract class ArrowheadResource {
    private final PublicKey authorizationKey;
    private final PrivateKey privateKey;

    public ArrowheadResource() {
        final Properties props = Utility.getProp("app.properties");

        //Load the Provider private key
        String keystorePath = props.getProperty("keystore");
        String keystorePass = props.getProperty("keystorepass");
        KeyStore keyStore = SecurityUtils.loadKeyStore(keystorePath, keystorePass);
        privateKey = SecurityUtils.getPrivateKey(keyStore, keystorePass);

        //Load the Authorization Core System public key
        String authCertPath = props.getProperty("authorization_cert");
        KeyStore authKeyStore = SecurityUtils.createKeyStoreFromCert(authCertPath);
        X509Certificate authCert = SecurityUtils.getFirstCertFromKeyStore(authKeyStore);
        authorizationKey = authCert.getPublicKey();
        System.out.println("Authorization CN: " + SecurityUtils.getCertCNFromSubject(authCert.getSubjectDN().getName()));
        System.out.println("Authorization System PublicKey Base64: " + Base64.getEncoder().encodeToString(authorizationKey.getEncoded()));
    }

    protected <T> Response verifiedResponse(SecurityContext context, String token, String signature, T readout) {
        return context.isSecure() ?
                verifyRequester(context, token, signature, () -> readout) :
                Response.status(200).entity(readout).build();
    }

    protected <T> Response verifiedResponse(SecurityContext context, String token, String signature, Supplier<T> onOk) {
        return context.isSecure() ?
                verifyRequester(context, token, signature, onOk) :
                Response.status(200).entity(onOk).build();
    }

    /*
      Based on the local Authorization public key and provider private key, this method verifies that the provided token/signature pair
      was created by the Authorization Core System with the provider public key. It also checks if the token expired or not, plus the token
      has to contain the same consumer name as the common name field of the client certificate.
     */
    private <T> Response verifyRequester(SecurityContext context, String token, String signature, Supplier<T> onOk) {
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
            signatureInstance.initVerify(authorizationKey);
            signatureInstance.update(tokenbytes);

            boolean verifies = signatureInstance.verify(signaturebytes);
            if (!verifies) {
                ErrorMessage error = new ErrorMessage("Token validation failed", 401, ExceptionType.AUTH, Utility.class.toString());
                return Response.status(401).entity(error).build();
            }

            Cipher cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding", "BC");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            //Check if the provider public key registered in the database is the same as the one used by the provider at the moment
            byte[] byteToken = cipher.doFinal(tokenbytes);

            String json = new String(byteToken, "UTF-8");
            RawTokenInfo rawTokenInfo = Utility.fromJson(json, RawTokenInfo.class);
            String[] rawTokenInfoParts = rawTokenInfo.getC().split("\\.");
            String consumerTokenName = rawTokenInfoParts[0];

            long endTime = rawTokenInfo.getE();
            long currentTime = System.currentTimeMillis();

            if (consumerName.equals(consumerTokenName)) {
                if (endTime == 0L || (endTime > currentTime)) {
                    return Response.status(200).entity(onOk.get()).build();
                }
                ErrorMessage error = new ErrorMessage("Authorization token has expired", 401, ExceptionType.AUTH, Utility.class.toString());
                return Response.status(401).entity(error).build();

            } else {
                ErrorMessage error = new ErrorMessage("Permission denied", 401, ExceptionType.AUTH, Utility.class.toString());
                return Response.status(401).entity(error).build();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            ErrorMessage error = new ErrorMessage("Internal Server Error: " + ex.getMessage(), 500, null, Utility.class.toString());
            return Response.status(500).entity(error).build();
        }
    }
}
