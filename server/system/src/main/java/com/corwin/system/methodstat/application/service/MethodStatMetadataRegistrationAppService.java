package com.corwin.system.methodstat.application.service;

import com.corwin.system.methodstat.domain.model.MethodStatMetadata;
import com.corwin.system.methodstat.domain.model.MethodStatMethodDescriptor;
import com.corwin.system.methodstat.domain.model.MethodStatMethodSwitchState;
import com.corwin.system.methodstat.domain.repo.MethodStatMetadataRepository;
import com.corwin.system.methodstat.domain.repo.MethodStatSwitchStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author Corwin 2026/3/25
 */
@Service
@RequiredArgsConstructor
public class MethodStatMetadataRegistrationAppService {

    private final MethodStatMetadataRepository metadataRepository;
    private final MethodStatSwitchStateRepository switchStateRepository;

    public MethodStatMetadata register(MethodStatMethodDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor required");

        boolean methodSwitchEnabled = ensureMethodSwitchState(descriptor);
        MethodStatMetadata metadata = new MethodStatMetadata(descriptor.key(), descriptor.packageName(),
                descriptor.className(), descriptor.methodName(), descriptor.methodSignature(), methodSwitchEnabled);
        return metadataRepository.save(metadata);
    }

    private boolean ensureMethodSwitchState(MethodStatMethodDescriptor descriptor) {
        return switchStateRepository.findMethodSwitchState(descriptor.key()).map(MethodStatMethodSwitchState::enabled)
                .orElseGet(() -> {
                    switchStateRepository.saveMethodSwitchState(
                            new MethodStatMethodSwitchState(descriptor.key(), false));
                    return false;
                });
    }
}
