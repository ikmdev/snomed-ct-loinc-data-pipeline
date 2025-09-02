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
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.stream.Stream;

public class ConceptTransformer extends AbstractTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(ConceptTransformer.class.getSimpleName());
    private static final int ID = 0;
    private static final int EFFECTIVE_TIME = 1;
    private static final int ACTIVE = 2;
    private static final int MODULE_ID = 3;
    private static final int DEFINITION_STATUS_ID = 4;
    private String previousRowId;

    ConceptTransformer(UUID namespace) {
        super(namespace);
    }

    /**
     * transforms concept file into entity
     * @param inputFile concept input txt file
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
                    configureIdentifier(session, data[ID]);
                });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * setting up concept data to be transformed
     * @param session transforms OWL snomed file into entities
     * @param rowId represents each row in concept file
     */
    private void configureIdentifier(Session session, String rowId) {
        PublicId publicId = PublicIds.of(SnomedLoincUtility.generateUUID(namespace, rowId));
        // TODO: Refactor programmatically linking/merging concepts
//        if (rowId.equals("138875005")) { // Link SnomedCT Root Concept with Snomed CT Starter Data Health Concept
//            publicId = PublicIds.of(new UUIDUtility().createUUID("SNOMED CT Concept"), UuidUtil.fromSNOMED(rowId));
//        }
//        if (rowId.equals("609096000")) { // Link SnomedCT Role Group Concept with Tinkar Starter Data Role Group
//            publicId = PublicIds.of(TinkarTerm.ROLE_GROUP.asUuidArray()[0], UuidUtil.fromSNOMED(rowId));
//        }
        EntityProxy.Concept concept = EntityProxy.Concept.make(publicId);

        if (!rowId.equals(previousRowId)) {
            session.compose((ConceptAssembler conceptAssembler) -> conceptAssembler
                    .concept(concept)
                    .attach((Identifier identifier) -> identifier
                            .source(TinkarTerm.UNIVERSALLY_UNIQUE_IDENTIFIER)
                            .identifier(concept.asUuidArray()[0].toString())
                    )
                    .attach((Identifier identifier) -> identifier
                            .source(TinkarTerm.SCTID)
                            .identifier(rowId)
                    )
            );
        }
        previousRowId = rowId;
    }
}