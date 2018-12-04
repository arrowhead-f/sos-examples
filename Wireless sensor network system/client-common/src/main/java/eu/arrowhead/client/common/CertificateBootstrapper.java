package eu.arrowhead.client.common;

import eu.arrowhead.client.common.exception.ArrowheadException;
import eu.arrowhead.client.common.exception.AuthException;
import eu.arrowhead.client.common.misc.ClientType;
import eu.arrowhead.client.common.misc.SecurityUtils;
import eu.arrowhead.client.common.misc.TypeSafeProperties;
import eu.arrowhead.client.common.model.CertificateSigningRequest;
import eu.arrowhead.client.common.model.CertificateSigningResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceConfigurationError;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;

public final class CertificateBootstrapper {

  private static TypeSafeProperties props = Utility.getProp();
  private static String CA_URL = props.getProperty("cert_authority_url");

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  private CertificateBootstrapper() {
    throw new AssertionError("CertificateBootstrapper is a non-instantiable class");
  }

  public static SSLContextConfigurator bootstrap(ClientType clientType, String systemName) {
    //Check if the CA is available at the provided URL (with socket opening)
    URL url;
    try {
      url = new URL(CA_URL);
    } catch (MalformedURLException e) {
      throw new ArrowheadException(
          "cert_authority_url property is not (properly) provided in config file, but certificate bootstrapping is requested!",
          Status.BAD_REQUEST.getStatusCode(), e);
    }
    if (!Utility.isHostAvailable(url.getHost(), url.getPort(), 3000)) {
      throw new ArrowheadException("CA Core System is unavailable at " + props.getProperty("cert_authority_url"));
    }

    //Prepare the data needed to generate the certificate(s)
    String cloudCN = CertificateBootstrapper.getCloudCommonNameFromCA();
    systemName = systemName != null ? systemName : clientType.name().replaceAll("_", "").toLowerCase() + System.currentTimeMillis();
    String keyStorePassword = !Utility.isBlank(props.getProperty("keystorepass")) ? props.getProperty("keystorepass") : Utility.getRandomPassword();
    String trustStorePassword =
        !Utility.isBlank(props.getProperty("truststorepass")) ? props.getProperty("truststorepass") : Utility.getRandomPassword();

    //Obtain the keystore and truststore
    KeyStore[] keyStores = CertificateBootstrapper
        .obtainSystemAndCloudKeyStore(systemName, cloudCN, keyStorePassword.toCharArray(), trustStorePassword.toCharArray());

    //Save the keystores to file
    String certPathPrefix = "config" + File.separator + "certificates";
    CertificateBootstrapper.saveKeyStoreToFile(keyStores[0], keyStorePassword.toCharArray(), systemName + ".p12", certPathPrefix);
    CertificateBootstrapper.saveKeyStoreToFile(keyStores[1], trustStorePassword.toCharArray(), "truststore.p12", certPathPrefix);

    //Update app.conf with the new values
    Map<String, String> secureParameters = new HashMap<>();
    secureParameters.put("keystore", certPathPrefix + File.separator + systemName + ".p12");
    secureParameters.put("keystorepass", keyStorePassword);
    secureParameters.put("keypass", keyStorePassword);
    secureParameters.put("truststore", certPathPrefix + File.separator + "truststore.p12");
    secureParameters.put("truststorepass", trustStorePassword);
    if (clientType.equals(ClientType.PROVIDER)) {
      getAuthorizationPublicKey(certPathPrefix + File.separator + "authorization.pub");
      secureParameters.put("authorization_public_key", certPathPrefix + File.separator + "authorization.pub");
    }
    CertificateBootstrapper.updateConfigurationFiles("config" + File.separator + "app.conf", secureParameters);

    //Return a new, valid SSLContextConfigurator
    SSLContextConfigurator sslCon = new SSLContextConfigurator();
    sslCon.setKeyStoreFile(certPathPrefix + File.separator + systemName + ".p12");
    sslCon.setKeyStorePass(keyStorePassword);
    sslCon.setKeyPass(keyStorePassword);
    sslCon.setTrustStoreFile(certPathPrefix + File.separator + "truststore.p12");
    sslCon.setTrustStorePass(trustStorePassword);
    return sslCon;
  }

  /*
    Gets the Cloud Common Name from the Certificate Authority Core System, proper URL is read from the config file
   */
  private static String getCloudCommonNameFromCA() {
    Response caResponse = Utility.sendRequest(CA_URL, "GET", null);
    return caResponse.readEntity(String.class);
  }

  /**
   * Get a KeyStore with an Arrowhead compliant certificate chain for an Application System from the Certificate Authority
   *
   * @param commonName will be used at the CN (Common Name) field of the certificate, this has to be Arrowhead compliant: &lt;system_name&gt;
   *     .&lt;cloud_name&gt;.&lt;operator&gt;.arrowhead.eu
   * @param keyStorePassword the password to load the KeyStore and to protect the private key
   *
   * @return the constructed KeyStore
   *
   * @see <a href="https://tools.ietf.org/html/rfc5280.html#section-7.1">X.509 certificate specification: distinguished names</a>
   */
  @SuppressWarnings("unused")
  private static KeyStore obtainSystemKeyStore(String commonName, char[] keyStorePassword) {
    CertificateSigningResponse signingResponse = getSignedCertFromCA(commonName);

    //Get the reconstructed certs from the CA response
    X509Certificate signedCert = getCertFromString(signingResponse.getEncodedSignedCert());
    X509Certificate cloudCert = getCertFromString(signingResponse.getIntermediateCert());
    X509Certificate rootCert = getCertFromString(signingResponse.getRootCert());

    //Create the new KeyStore
    try {
      KeyStore ks = KeyStore.getInstance("pkcs12");
      ks.load(null, keyStorePassword);
      Certificate[] chain = new Certificate[]{signedCert, cloudCert, rootCert};
      ks.setKeyEntry(commonName, signingResponse.getLocalPrivateKey(), keyStorePassword, chain);
      return ks;
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      throw new ArrowheadException("KeyStore creation failed!", e);
    }
  }

  /**
   * Gets
   * 1) a KeyStore with an Arrowhead compliant certificate chain for an Application System
   * 2) a KeyStore containing a single LocalCloud level certificate (which is issued my the Arrowhead master certificate), without a private key
   * <p>
   * from the local Certificate authority.
   *
   * @param systemName Name of the application system needing the new certificate (will be used in the common name field)
   * @param cloudCN LocalCloud level common name: &lt;cloud_name&gt;.&lt;operator&gt;.arrowhead.eu
   * @param systemKsPassword password for the application system keystore
   * @param cloudKsPassword password for the cloud keystore
   *
   * @return the constructed KeyStores in an array,
   *
   * @see <a href="https://tools.ietf.org/html/rfc5280.html#section-7.1">X.509 certificate specification: distinguished names</a>
   */
  private static KeyStore[] obtainSystemAndCloudKeyStore(String systemName, String cloudCN, char[] systemKsPassword, char[] cloudKsPassword) {
    String commonName = systemName + "." + cloudCN;
    CertificateSigningResponse signingResponse = getSignedCertFromCA(commonName);

    //Get the reconstructed certs from the CA response
    X509Certificate signedCert = getCertFromString(signingResponse.getEncodedSignedCert());
    X509Certificate cloudCert = getCertFromString(signingResponse.getIntermediateCert());
    X509Certificate rootCert = getCertFromString(signingResponse.getRootCert());

    //Create the System KeyStore
    KeyStore[] keyStores = new KeyStore[2];
    try {
      KeyStore ks = KeyStore.getInstance("pkcs12");
      ks.load(null, systemKsPassword);
      Certificate[] chain = new Certificate[]{signedCert, cloudCert, rootCert};
      ks.setKeyEntry(commonName, signingResponse.getLocalPrivateKey(), systemKsPassword, chain);
      keyStores[0] = ks;
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      throw new ArrowheadException("System key store creation failed!", e);
    }

    /*
      Create the Cloud KeyStore (with a different KeyStore Entry type,
      since we do not have the private key for the cloud cert)
     */
    try {
      KeyStore ks = KeyStore.getInstance("pkcs12");
      ks.load(null, cloudKsPassword);
      KeyStore.Entry certEntry = new KeyStore.TrustedCertificateEntry(cloudCert);
      ks.setEntry(cloudCN, certEntry, null);
      keyStores[1] = ks;
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      throw new ArrowheadException("System key store creation failed!", e);
    }

    return keyStores;
  }

  /**
   * Saves the provided KeyStore to a file
   *
   * @param keyStore the certificate storage facility needed to be saved to file
   * @param keyStorePassword password to open the keystore
   * @param fileName filename must end with a valid keystore file extension (p12 or jks)
   * @param saveLocation optional relative or absolute path where the keystore should be saved (must point to directory). If not provided, the file
   *     will be placed into the working directory.
   */
  private static void saveKeyStoreToFile(KeyStore keyStore, char[] keyStorePassword, String fileName, String saveLocation) {
    if (keyStore == null || fileName == null) {
      throw new NullPointerException("Saving the key store to file is not possible, key store or file name is null!");
    }
    if (!(fileName.endsWith(".p12") || fileName.endsWith(".jks"))) {
      throw new ServiceConfigurationError("File name should end with its extension! (p12 or jks)");
    }
    if (saveLocation != null) {
      fileName = saveLocation + File.separator + fileName;
    }
    try (FileOutputStream fos = new FileOutputStream(fileName)) {
      keyStore.store(fos, keyStorePassword);
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      throw new ArrowheadException("Saving keystore to file " + fileName + " failed!", e);
    }
  }

  /*
     Updates the given properties file with the given key-value pairs.
   */
  private static void updateConfigurationFiles(String configLocation, Map<String, String> configValues) {
    try {
      FileInputStream in = new FileInputStream(configLocation);
      TypeSafeProperties props = new TypeSafeProperties();
      props.load(in);
      in.close();

      FileOutputStream out = new FileOutputStream(configLocation);
      for (Entry<String, String> entry : configValues.entrySet()) {
        props.setProperty(entry.getKey(), entry.getValue());
      }
      props.store(out, null);
      out.close();
    } catch (IOException e) {
      throw new ArrowheadException("Cert bootstrapping: IOException during configuration file update", e);
    }
    props = Utility.getProp();
    CA_URL = props.getProperty("cert_authority_url");
  }

  /*
    Authorization Public Key is used by ArrowheadProviders to verify the signatures by the Authorization Core System in secure mode
   */
  private static void getAuthorizationPublicKey(String filePath) {
    Response caResponse = Utility.sendRequest(CA_URL + "/auth", "GET", null);
    try (FileOutputStream fos = new FileOutputStream(filePath)) {
      OutputStreamWriter osw = new OutputStreamWriter(fos);
      JcaPEMWriter pemWriter = new JcaPEMWriter(osw);
      PublicKey publicKey = SecurityUtils.getPublicKey(caResponse.readEntity(String.class), false);
      pemWriter.writeObject(publicKey);
      pemWriter.flush();
      pemWriter.close();
      osw.close();
    } catch (IOException e) {
      throw new ArrowheadException("IO exception during Authorization public key save!", e);
    }
  }

  //Generate a new 2048 bit RSA key pair
  private static KeyPair generateRSAKeyPair() {
    KeyPairGenerator keyGen;
    try {
      keyGen = KeyPairGenerator.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new ServiceConfigurationError("KeyPairGenerator has no RSA algorithm", e);
    }
    keyGen.initialize(2048);
    return keyGen.generateKeyPair();
  }

  private static CertificateSigningResponse getSignedCertFromCA(String commonName) {
    //Get a new locally generated public/private key pair
    KeyPair keyPair = generateRSAKeyPair();

    //Create the PKCS10 certificate request (signed by private key)
    ContentSigner signer;
    try {
      signer = new JcaContentSignerBuilder("SHA512withRSA").setProvider("BC").build(keyPair.getPrivate());
    } catch (OperatorCreationException e) {
      throw new AuthException("Certificate request signing failed! (" + e.getMessage() + ")", e);
    }
    PKCS10CertificationRequest csr = new JcaPKCS10CertificationRequestBuilder(new X500Name("CN=" + commonName), keyPair.getPublic()).build(signer);

    //Encode the CSR, and send it to the Certificate Authority core system
    String encodedCertRequest;
    try {
      encodedCertRequest = Base64.getEncoder().encodeToString(csr.getEncoded());
    } catch (IOException e) {
      throw new AuthException("Failed to encode certificate signing request!", e);
    }
    CertificateSigningRequest request = new CertificateSigningRequest(encodedCertRequest);
    Response caResponse = Utility.sendRequest(CA_URL, "POST", request);
    CertificateSigningResponse signingResponse = caResponse.readEntity(CertificateSigningResponse.class);
    signingResponse.setLocalPrivateKey(keyPair.getPrivate());
    return signingResponse;
  }

  //Convert PEM encoded cert back to an X509Certificate
  @SuppressWarnings("Duplicates")
  private static X509Certificate getCertFromString(String encodedCert) {
    try {
      byte[] rawCert = Base64.getDecoder().decode(encodedCert);
      ByteArrayInputStream bIn = new ByteArrayInputStream(rawCert);
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      return (X509Certificate) cf.generateCertificate(bIn);
    } catch (CertificateException e) {
      throw new AuthException("Encapsulated exceptions...", e);
    }
  }

}
