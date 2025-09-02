/*
 * Copyright © 2015 IKM (support@ikm.dev)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.ikm.maven;

import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.common.util.uuid.UuidUtil;
import dev.ikm.tinkar.composer.Composer;
import dev.ikm.tinkar.composer.Session;
import dev.ikm.tinkar.composer.assembler.SemanticAssembler;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

public class LanguageTransformer extends AbstractTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageTransformer.class.getSimpleName());
    private static final int ID = 0;
    private static final int EFFECTIVE_TIME = 1;
    private static final int ACTIVE = 2;
    private static final int MODULE_ID = 3;
    private static final int REFSET_ID = 4;
    private static final int REFERENCED_COMPONENT_ID = 5;
    private static final int ACCEPTABILITY_ID = 6;
    LanguageTransformer(UUID namespace) {
        super(namespace);
    }

    /**
     * This method uses a language file and transforms it into a list of entities
     * @param inputFile text file to be transformed
     * @Returns EntityList
     */
    @Override
    public void transform(File inputFile, Composer composer) {
        EntityProxy.Concept author = SnomedLoincUtility.getUserConcept(namespace);
        EntityProxy.Concept path = SnomedLoincUtility.getPathConcept();

        try (Stream<String> lines = Files.lines(inputFile.toPath())) {
            lines.skip(1) //skip first line, i.e. header line
                .map(row -> row.split("\t"))
                .forEach((data) -> {
                    State status = Integer.parseInt(data[ACTIVE]) == 1 ? State.ACTIVE : State.INACTIVE;
                    long epochTime = SnomedLoincUtility.snomedTimestampToEpochSeconds(data[EFFECTIVE_TIME]);
                    EntityProxy.Concept moduleId = EntityProxy.Concept.make(PublicIds.of(SnomedLoincUtility.generateUUID(namespace, data[MODULE_ID])));

                    Session session = composer.open(status, epochTime, author, moduleId, path);

                    EntityProxy.Concept referencedComponent = EntityProxy.Concept.make(PublicIds.of(SnomedLoincUtility.generateUUID(namespace, data[REFERENCED_COMPONENT_ID])));

                    session.compose((SemanticAssembler semanticAssembler) -> semanticAssembler
                            .pattern(SnomedLoincUtility.getDialectPattern(data[REFSET_ID]))
                            .reference(referencedComponent)
                            .fieldValues(fieldValues -> fieldValues
                                    .with(SnomedLoincUtility.getDialectAccceptability(data[ACCEPTABILITY_ID]))
                            ));
                 });
        } catch(IOException e) {
             LOG.warn("Error parsing language file");
        }
    }
}
