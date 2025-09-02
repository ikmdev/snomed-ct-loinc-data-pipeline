package dev.ikm.maven;

import java.util.UUID;

public abstract class AbstractTransformer implements Transformer {
    final UUID namespace;
    AbstractTransformer(UUID namespace) {
        this.namespace = namespace;
    }
    @Override
    public UUID getNamespace() {
        return namespace;
    }
}
