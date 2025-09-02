package dev.ikm.maven;

import dev.ikm.tinkar.composer.Composer;

import java.io.File;
import java.util.UUID;

public interface Transformer {
    void transform(File file, Composer composer);
    UUID getNamespace();
}
