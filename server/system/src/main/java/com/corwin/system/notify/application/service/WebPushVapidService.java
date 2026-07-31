package com.corwin.system.notify.application.service;

import com.corwin.framework.config.runtime.Configs;
import com.corwin.system.notify.config.SystemNotifyConfigSpecs;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.security.Security;

/**
 * @author Corwin 2026/3/19
 */
@Service
public class WebPushVapidService {

    /**
     * Retained for callers that only need provider readiness. Key generation is intentionally disabled.
     */
    public void ensureVapidKeys() {
        ensureBouncyCastleProvider();
    }

    public String getPublicKey() {
        return Configs.get(SystemNotifyConfigSpecs.WEB_PUSH).publicKey();
    }

    public PushService newPushService() throws GeneralSecurityException {
        ensureBouncyCastleProvider();
        var config = Configs.get(SystemNotifyConfigSpecs.WEB_PUSH);
        if (config.publicKey().isBlank() || config.privateKey().isBlank()) {
            throw new GeneralSecurityException("Web Push VAPID keys are not configured");
        }
        return new PushService(config.publicKey(), config.privateKey(), config.subject());
    }

    public void ensureProviderReady() {
        ensureBouncyCastleProvider();
    }

    private void ensureBouncyCastleProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
}
