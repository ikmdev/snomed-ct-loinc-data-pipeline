package dev.ikm.maven;

import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.common.util.uuid.UuidT5Generator;
import dev.ikm.tinkar.composer.Composer;
import dev.ikm.tinkar.composer.Session;
import dev.ikm.tinkar.composer.assembler.ConceptAssembler;
import dev.ikm.tinkar.composer.template.*;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.State;
import dev.ikm.tinkar.terms.TinkarTerm;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.UUID;

import static dev.ikm.tinkar.terms.TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE;
import static dev.ikm.tinkar.terms.TinkarTerm.ENGLISH_LANGUAGE;

/**
 * Hello world!
 *
 */
@Mojo(name = "run-snomed-ct-loinc-starterdata", defaultPhase = LifecyclePhase.INSTALL)
public class SnomedctLoincStarterDataMojo extends AbstractMojo
{
    @Parameter(property = "origin.namespace", required = true)
    String namespaceString;
    @Parameter(property = "datastorePath", required = true)
    private String datastorePath;
    @Parameter(property = "controllerName", defaultValue = "Open SpinedArrayStore")
    private String controllerName;

    private UUID namespace;
    public void execute() throws MojoExecutionException
    {
        try {
            this.namespace = UUID.fromString(namespaceString);
            File datastore = new File(datastorePath);

            CachingService.clearAll();
            ServiceProperties.set(ServiceKeys.DATA_STORE_ROOT, datastore);
            PrimitiveData.selectControllerByName(controllerName);
            PrimitiveData.start();

            Composer composer = new Composer("Snomed CT Loinc Starter Data Composer");

            EntityProxy.Concept snomedLoincAuthor = EntityProxy.Concept.make("SNOMED CT LOINC Collaboration Starter Data Author", UuidT5Generator.get(namespace, "SNOMED CT LOINC Collaboration Starter Data Author"));
            Session session = composer.open(State.ACTIVE,
                    snomedLoincAuthor,
                    TinkarTerm.PRIMORDIAL_MODULE,
                    TinkarTerm.PRIMORDIAL_PATH);

            session.compose((ConceptAssembler concept) -> concept
                    .concept(snomedLoincAuthor)
                    .attach((FullyQualifiedName fqn) -> fqn
                            .language(ENGLISH_LANGUAGE)
                            .text("IHTSDO SNOMED CT LOINC Collaboration Starter Data Author")
                            .caseSignificance(DESCRIPTION_NOT_CASE_SENSITIVE)
                    )
                    .attach((Synonym synonym)-> synonym
                            .language(ENGLISH_LANGUAGE)
                            .text("SNOMED CT LOINC Collaboration Starter Data Author")
                            .caseSignificance(DESCRIPTION_NOT_CASE_SENSITIVE)
                    )
                    .attach((Definition definition) -> definition
                            .language(ENGLISH_LANGUAGE)
                            .text("International Health Terminology Standards Development Organisation (IHTSDO) SNOMED CT LOINC Collaboration Starter Data Author")
                            .caseSignificance(DESCRIPTION_NOT_CASE_SENSITIVE)
                    )
                    .attach((Identifier identifier) -> identifier
                            .source(TinkarTerm.UNIVERSALLY_UNIQUE_IDENTIFIER)
                            .identifier(snomedLoincAuthor.asUuidArray()[0].toString())
                    )
                    .attach((StatedAxiom statedAxiom) -> statedAxiom
                            .isA(TinkarTerm.USER)
                    )
            );

            composer.commitSession(session);
            PrimitiveData.stop();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to execute class", e);
        }

    }
}
