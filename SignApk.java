import com.android.apksig.ApkSigner;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

public class SignApk {
    public static void main(String[] args) throws Exception {
        File inApk = new File(args[0]);
        File outApk = new File(args[1]);
        File ksFile = new File(args[2]);
        String ksPass = args[3];
        String alias = args[4];

        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(ksFile)) {
            ks.load(fis, ksPass.toCharArray());
        }

        PrivateKey key = (PrivateKey) ks.getKey(alias, ksPass.toCharArray());
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

        ApkSigner.SignerConfig signerConfig = new ApkSigner.SignerConfig.Builder(
                "CERT", key, Collections.singletonList(cert)).build();

        ApkSigner.Builder builder = new ApkSigner.Builder(Collections.singletonList(signerConfig))
                .setInputApk(inApk)
                .setOutputApk(outApk)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true);

        builder.build().sign();
        System.out.println("Signed APK with v1, v2, v3 schemes successfully!");
    }
}
