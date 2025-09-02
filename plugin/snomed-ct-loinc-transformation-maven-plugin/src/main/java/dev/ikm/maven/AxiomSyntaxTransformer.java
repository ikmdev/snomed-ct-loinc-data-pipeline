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
import dev.ikm.tinkar.composer.template.AxiomSyntax;
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

/**
 * Class to parse OWL Expression files and create Axiom Syntax Semantics
 */
public class AxiomSyntaxTransformer extends AbstractTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(AxiomSyntaxTransformer.class.getSimpleName());
    private static final int ID = 0;
    private static final int EFFECTIVE_TIME = 1;
    private static final int ACTIVE = 2;
    private static final int MODULE_ID = 3;
    private static final int REFSET_ID = 4;
    private static final int REFERENCED_COMPONENT_ID = 5;
    private static final int OWL_EXPRESSION = 6;
    String previousRowId;
    AxiomSyntaxTransformer(UUID namespace) {
        super(namespace);
    }

    /**
     * Parses OWL Expression file and creates Axiom Semantics for each line
     *
     * @param axiomFile input file Path
     * @param composer composer utility
     */
    @Override
    public void transform(File axiomFile, Composer composer) {

        EntityProxy.Concept author = SnomedLoincUtility.getUserConcept(namespace);
        EntityProxy.Concept path = SnomedLoincUtility.getPathConcept();

        try (Stream<String> lines = Files.lines(axiomFile.toPath())) {
            lines.skip(1)
                .forEach(line ->{
                   String[] columns = line.split("\t");
                   long time = SnomedLoincUtility.snomedTimestampToEpochSeconds(columns[EFFECTIVE_TIME]);
                   State status = columns[ACTIVE].equals("1") ? State.ACTIVE : State.INACTIVE;
                   EntityProxy.Concept module = EntityProxy.Concept.make(PublicIds.of(SnomedLoincUtility.generateUUID(namespace, columns[MODULE_ID])));

                   Session session = composer.open(status, time, author, module, path);
                   String owlExpressionWithPublicIds = SnomedLoincUtility.owlAxiomIdsToPublicIds(namespace, columns[OWL_EXPRESSION]);

                   EntityProxy.Concept concept = EntityProxy.Concept.make(PublicIds.of(SnomedLoincUtility.generateUUID(namespace, columns[REFERENCED_COMPONENT_ID])));
                   EntityProxy.Semantic axiomSemantic = EntityProxy.Semantic.make(PublicIds.of(SnomedLoincUtility.generateUUID(namespace, columns[ID])));

                   previousRowId = columns[ID];

                   session.compose(new AxiomSyntax()
                                    .semantic(axiomSemantic)
                                    .text(owlExpressionWithPublicIds),
                           concept);
               });
        } catch(IOException | SecurityException ex) {
            LOG.info(ex.toString());
        }
    }

}
