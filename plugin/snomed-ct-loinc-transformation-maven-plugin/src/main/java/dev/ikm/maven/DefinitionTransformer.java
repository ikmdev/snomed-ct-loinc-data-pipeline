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
import dev.ikm.tinkar.composer.assembler.SemanticAssembler;
import dev.ikm.tinkar.entity.Entity;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.EntityVersion;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.EntityProxy.Concept;
import dev.ikm.tinkar.terms.State;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

/**
 * Class to parse Definition files and create Definition Semantics
 */

public class DefinitionTransformer extends AbstractTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(DefinitionTransformer.class.getSimpleName());
    private static final int ID = 0;
    private static final int EFFECTIVE_TIME = 1;
    private static final int ACTIVE = 2;
    private static final int MODULE_ID = 3;
    private static final int CONCEPT_ID = 4;
    private static final int LANGUAGE_CODE = 5;
    private static final int TYPE_ID = 6;
    private static final int TERM = 7;
    private static final int CASE_SIGNIFICANCE_ID = 8;
    private static String previousRowId;
    private static Concept previousReferencedConcept;
    private static EntityProxy.Semantic previousDefinitionSemantic;
    DefinitionTransformer(UUID namespace) {
        super(namespace);
    }


    /**
     * Parses Definition file and creates Definition Semantics for each line
     *
     * @param definitionFile input file to parse
     */
    @Override
    public void transform(File definitionFile, Composer composer) {
        Concept author = SnomedLoincUtility.getUserConcept(namespace);
        Concept path = SnomedLoincUtility.getPathConcept();
        try (Stream<String> lines = Files.lines(definitionFile.toPath())) {
            lines.skip(1) //skip first line, i.e. header line
                    .map(row -> row.split("\t"))
                    .forEach(data -> {
                        State status = Integer.parseInt(data[ACTIVE]) == 1 ? State.ACTIVE : State.INACTIVE;
                        long epochTime = SnomedLoincUtility.snomedTimestampToEpochSeconds(data[EFFECTIVE_TIME]);
                        EntityProxy.Concept moduleConcept = EntityProxy.Concept.make(PublicIds.of(SnomedLoincUtility.generateUUID(namespace, data[MODULE_ID])));
                        Session session = composer.open(status, epochTime, author, moduleConcept, path);

                        Concept referencedConcept = previousReferencedConcept;
                        EntityProxy.Semantic definitionSemantic;

                        if (!data[ID].equals(previousRowId)) {
                            referencedConcept = Concept.make(PublicIds.of(SnomedLoincUtility.generateUUID(namespace, data[CONCEPT_ID])));

                            PublicId definitionPublicId = PublicIds.of(SnomedLoincUtility.generateUUID(namespace, data[ID]));
                            definitionSemantic = EntityProxy.Semantic.make(definitionPublicId);

                            previousRowId = data[ID];
                            previousReferencedConcept = referencedConcept;
                            previousDefinitionSemantic = definitionSemantic;
                        } else {
                            definitionSemantic = previousDefinitionSemantic;
                        }

                        Concept languageConcept = SnomedLoincUtility.getLanguageConcept(data[LANGUAGE_CODE]);
                        Concept caseSignificanceConcept = SnomedLoincUtility.getDescriptionCaseSignificanceConcept(data[CASE_SIGNIFICANCE_ID]);
                        Concept descriptionTypeConcept = SnomedLoincUtility.getDescriptionType(data[TYPE_ID]);

                        session.compose((SemanticAssembler semanticAssembler) -> semanticAssembler
                                .semantic(definitionSemantic)
                                .pattern(TinkarTerm.DESCRIPTION_PATTERN)
                                .reference(previousReferencedConcept)
                                .fieldValues(fieldValues -> fieldValues
                                        .with(languageConcept)
                                        .with(data[TERM])
                                        .with(caseSignificanceConcept)
                                        .with(descriptionTypeConcept)
                                ));
                    });
        } catch (IOException | SecurityException ex) {
            LOG.info(ex.toString());
        }
    }
}
