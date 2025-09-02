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


import dev.ikm.tinkar.common.id.PublicId;
import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.common.util.uuid.UuidUtil;
import dev.ikm.tinkar.composer.Composer;
import dev.ikm.tinkar.composer.Session;
import dev.ikm.tinkar.composer.assembler.ConceptAssembler;
import dev.ikm.tinkar.composer.template.Identifier;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.State;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.UUID;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

public class IdentifierTransformer extends AbstractTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(ConceptTransformer.class.getSimpleName());
    private static final int REFCOMPID = 5;
    private static final int ID = 0;
    private static final int EFFECTIVE_TIME = 1;
    private static final int ACTIVE = 2;
    private static final int MODULE_ID = 3;
    private static final int IDENTIFIER_SCHEME_ID = 4;
    private String previousRowId;

    IdentifierTransformer(UUID namespace) {super(namespace);}

    /**
     * transforms identifier file into entity
     * @param inputFile identifier input txt file
     */
    @Override
    public void transform(File inputFile, Composer composer){
        EntityProxy.Concept author = SnomedLoincUtility.getUserConcept(namespace);
        EntityProxy.Concept path = SnomedLoincUtility.getPathConcept();

        try (Stream<String> lines = Files.lines(inputFile.toPath())) {
            lines.skip(1) //skip first line, i.e. header line
                    .map(row -> row.split("\t"))
                    .forEach(data -> {
                        State status = Integer.parseInt(data[ACTIVE]) == 1 ? State.ACTIVE : State.INACTIVE;
                        long time = SnomedLoincUtility.snomedTimestampToEpochSeconds(data[EFFECTIVE_TIME]);
                        EntityProxy.Concept moduleIdConcept = EntityProxy.Concept.make(PublicIds.of(SnomedLoincUtility.generateUUID(namespace, data[MODULE_ID])));
                        Session session = composer.open(status, time, author, moduleIdConcept, path);

                        PublicId publicId = PublicIds.of(SnomedLoincUtility.generateUUID(namespace, data[REFCOMPID]));
                        EntityProxy.Concept concept = EntityProxy.Concept.make(publicId);
                        if (!data[ID].equals(previousRowId)) {
                            session.compose((ConceptAssembler conceptAssembler) -> conceptAssembler
                                    .concept(concept)
                                    .attach((Identifier identifier) -> identifier
                                            .source(TinkarTerm.UNIVERSALLY_UNIQUE_IDENTIFIER)
                                            .identifier(concept.asUuidArray()[0].toString())
                                    )
                                    .attach((Identifier identifier) -> identifier
                                            .source(SnomedLoincUtility.getSnomedLoincIdentifierConcept(namespace, data[IDENTIFIER_SCHEME_ID]))
                                            .identifier(data[ID])
                                    )
                            );
                        }
                        previousRowId = data[ID];
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
