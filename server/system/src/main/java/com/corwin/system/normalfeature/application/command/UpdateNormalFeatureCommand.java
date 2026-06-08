package com.corwin.system.normalfeature.application.command;

import java.util.List;

/**
 * @author Corwin 2026/4/20
 */
public record UpdateNormalFeatureCommand(
        List<String> featureIds
) {
}
