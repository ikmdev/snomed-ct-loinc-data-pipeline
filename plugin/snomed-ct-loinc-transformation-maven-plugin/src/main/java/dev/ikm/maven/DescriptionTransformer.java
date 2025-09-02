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


public class DescriptionTransformer extends AbstractTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(DescriptionTransformer.class.getSimpleName());
    private static final int ID = 0;
    private static final int EFFECTIVE_TIME = 1;
    private static final int ACTIVE = 2;
    private static final int MODULE_ID = 3;
    private static final int CONCEPT_ID = 4;
    private static final int LANGUAGE_CODE = 5;
    private static final int TYPE_ID = 6;
    private static final int TERM = 7;
    private static final int CASE_SIGNIFICANCE = 8;

    private String previousRowId;
    DescriptionTransformer(UUID namespace) {
        super(namespace);
    }

    /**
     * This method uses a description file and transforms it into a list of entities
     * @param descriptionFile
     * @param composer
     * @Returns void
     */
    public void transform(File descriptionFile, Composer composer){

        EntityProxy.Concept author = SnomedLoincUtility.getUserConcept(namespace);
        EntityProxy.Concept path = SnomedLoincUtility.getPathConcept();

        try (Stream<String> lines = Files.lines(descriptionFile.toPath())) {
            lines.skip(1) //skip first line, i.e. header line
                .map(row -> row.split("\t"))
                .forEach(data -> {
                    State status = Integer.parseInt(data[ACTIVE]) == 1 ? State.ACTIVE : State.INACTIVE;
                    long time = SnomedLoincUtility.snomedTimestampToEpochSeconds(data[EFFECTIVE_TIME]);
                    EntityProxy.Concept moduleId = EntityProxy.Concept.make(PublicIds.of(SnomedLoincUtility.generateUUID(namespace, data[MODULE_ID])));
                    Session session = composer.open(status, time, author, moduleId, path);
                    EntityProxy.Semantic descriptionSemantic = EntityProxy.Semantic.make(PublicIds.of(SnomedLoincUtility.generateUUID(namespace, data[ID])));

                    EntityProxy.Concept descriptionType = SnomedLoincUtility.getDescriptionType(data[TYPE_ID]);
                    EntityProxy.Concept languageType = SnomedLoincUtility.getLanguageConcept(data[LANGUAGE_CODE]);
                    EntityProxy.Concept caseSensitivityConcept = SnomedLoincUtility.getDescriptionCaseSignificanceConcept(data[CASE_SIGNIFICANCE]);

                    PublicId publicId = PublicIds.of(SnomedLoincUtility.generateUUID(namespace, data[CONCEPT_ID]));
                    EntityProxy.Concept concept = EntityProxy.Concept.make(publicId);

                    previousRowId = data[ID];

                    session.compose((SemanticAssembler semanticAssembler) -> semanticAssembler
                            .semantic(descriptionSemantic)
                            .pattern(TinkarTerm.DESCRIPTION_PATTERN)
                            .reference(concept)
                            .fieldValues(fieldValues -> fieldValues
                                    .with(languageType)
                                    .with(data[TERM])
                                    .with(caseSensitivityConcept)
                                    .with(descriptionType)
                            ));
                });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
