package com.corwin.system.notify.application.service;

import com.corwin.framework.config.ConfigRegistry;
import com.corwin.framework.config.ConfigStore;
import com.corwin.system.config.application.config.SystemConfigKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Base64Encoder;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.springframework.stereotype.Service;

import java.security.*;

/**
 * Web Push VAPID 密钥服务。
 *
 * @author Corwin 2026/3/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushVapidService {

    private final ConfigStore configStore;

    public synchronized void ensureVapidKeys() {
        ensureBouncyCastleProvider();

        String publicKey = ConfigRegistry.stringV(SystemConfigKeys.WEB_PUSH_VAPID_PUBLIC_KEY);
        String privateKey = ConfigRegistry.stringV(SystemConfigKeys.WEB_PUSH_VAPID_PRIVATE_KEY);
        if (!isBlank(publicKey) && !isBlank(privateKey)) {
            return;
        }

        KeyPair keyPair = generateKeyPair();
        String generatedPublic = Base64Encoder.encodeUrl(Utils.encode((ECPublicKey) keyPair.getPublic()));
        String generatedPrivate = Base64Encoder.encodeUrl(Utils.encode((ECPrivateKey) keyPair.getPrivate()));

        updateConfig(SystemConfigKeys.WEB_PUSH_VAPID_PUBLIC_KEY, generatedPublic);
        updateConfig(SystemConfigKeys.WEB_PUSH_VAPID_PRIVATE_KEY, generatedPrivate);
    }

    public String getPublicKey() {
        ensureVapidKeys();
        return ConfigRegistry.stringV(SystemConfigKeys.WEB_PUSH_VAPID_PUBLIC_KEY);
    }

    public PushService newPushService() throws GeneralSecurityException {
        ensureBouncyCastleProvider();
        ensureVapidKeys();
        String publicKey = ConfigRegistry.stringV(SystemConfigKeys.WEB_PUSH_VAPID_PUBLIC_KEY);
        String privateKey = ConfigRegistry.stringV(SystemConfigKeys.WEB_PUSH_VAPID_PRIVATE_KEY);
        String subject = ConfigRegistry.stringV(SystemConfigKeys.WEB_PUSH_VAPID_SUBJECT);
        return new PushService(publicKey, privateKey, subject);
    }

    public void ensureProviderReady() {
        ensureBouncyCastleProvider();
    }

    private KeyPair generateKeyPair() {
        try {
            ensureBouncyCastleProvider();
            ECNamedCurveParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("prime256v1");
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH",
                    BouncyCastleProvider.PROVIDER_NAME);
            keyPairGenerator.initialize(parameterSpec);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException ex) {
            throw new IllegalStateException("Generate VAPID key pair failed", ex);
        }
    }

    private void ensureBouncyCastleProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private void updateConfig(SystemConfigKeys configKey, String value) {
        boolean updated = configStore.updateValue(configKey.name(), value);
        if (!updated) {
            log.warn("VAPID config update skipped because key not found in sys_config: {}", configKey.name());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
