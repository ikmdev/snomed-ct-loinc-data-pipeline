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
import dev.ikm.tinkar.common.util.uuid.UuidT5Generator;
import dev.ikm.tinkar.common.util.uuid.UuidUtil;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.EntityProxy.Concept;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import dev.ikm.tinkar.common.util.time.DateTimeUtil;

public class SnomedLoincUtility {

    private static final Logger LOG = LoggerFactory.getLogger(SnomedLoincUtility.class.getSimpleName());


    /**
     * taking time stamp and making it an epoch
     * @param effectiveTime String representation dates in yyyyMMdd format
     * @return long value of epochTime
     */
    public static long snomedTimestampToEpochSeconds(String effectiveTime) {
    	return DateTimeUtil.compressedDateParse(effectiveTime);
    }

    /**
     * transforms languageCode in concept
     * @param languageCode String representation of english or spanish
     * @return language concept
     */
    public static Concept getLanguageConcept(String languageCode){
        Concept languageConcept = null;
        switch (languageCode) {
            case "en" -> languageConcept = TinkarTerm.ENGLISH_LANGUAGE;
            case "es" -> languageConcept = TinkarTerm.SPANISH_LANGUAGE;
            default -> throw new RuntimeException("UNRECOGNIZED LANGUAGE CODE");
        }
        return languageConcept;
    }

    /**
     * transforms caseSensitivity code into concept
     * @param caseSensitivityCode represents case sensitivity of a description
     * @return case sensitivity concept
     */
    public static Concept getDescriptionCaseSignificanceConcept(String caseSensitivityCode){
        Concept caseSensitivityConcept = null;
        switch (caseSensitivityCode) {
            case "900000000000448009" -> caseSensitivityConcept = TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE;
            case "900000000000017005" -> caseSensitivityConcept = TinkarTerm.DESCRIPTION_CASE_SENSITIVE;
            case "900000000000020002" -> caseSensitivityConcept = TinkarTerm.DESCRIPTION_INITIAL_CHARACTER_CASE_SENSITIVE;
            default -> throw new RuntimeException("UNRECOGNIZED CASE SENSITIVITY CODE");
        }
        return caseSensitivityConcept;
    }

    /**
     * transform descriptionType into concept
     * @param descriptionTypeCode String representation the type of descriptions
     * @return description type concept
     */
     public static Concept getDescriptionType(String descriptionTypeCode){
        Concept descriptionTypeConcept = null;
        switch (descriptionTypeCode) {
            case "900000000000550004" -> descriptionTypeConcept = TinkarTerm.DEFINITION_DESCRIPTION_TYPE;
            case "900000000000003001" -> descriptionTypeConcept = TinkarTerm.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE;
            case "900000000000013009" -> descriptionTypeConcept = TinkarTerm.REGULAR_NAME_DESCRIPTION_TYPE;
            default -> throw new RuntimeException("UNRECOGNIZED DESCRIPTION TYPE CODE");
        }
        return descriptionTypeConcept;
    }

    /**
     * assigns dialect pattern to TinkarTerm depending on dialectRefSetId
     * @param dialectRefsetId GB or US dialect semantic
     * @return dialect pattern
     */
    public static EntityProxy.Pattern getDialectPattern(String dialectRefsetId) {
        EntityProxy.Pattern dialectPattern = null;
        switch (dialectRefsetId) {
            case "900000000000509007" -> dialectPattern = TinkarTerm.US_DIALECT_PATTERN;
            case "900000000000508004" -> dialectPattern = TinkarTerm.GB_DIALECT_PATTERN;
            default -> throw new RuntimeException("UNRECOGNIZED ACCEPTABILITY CODE");
        }
        return dialectPattern;
    }

    /**
     * assigns acceptability code to TinkarTerm based on acceptabilityCode
     * @param acceptabilityCode preferred or acceptable
     * @return acceptability concept
     */
    public static Concept getDialectAccceptability(String acceptabilityCode){
        Concept acceptabilityConcept = null;
        switch (acceptabilityCode) {
            case "900000000000548007" -> acceptabilityConcept = TinkarTerm.PREFERRED;
            case "900000000000549004" -> acceptabilityConcept = TinkarTerm.ACCEPTABLE;
            default -> throw new RuntimeException("UNRECOGNIZED ACCEPTABILITY CODE");
        }
        return acceptabilityConcept;
    }

    /**
     * retrieves user concept
     * @return the snomed author
     */
        public static Concept getUserConcept(UUID namespace){
            Concept snomedLoincAuthor = Concept.make("SNOMED CT LOINC Collaboration Author", UuidT5Generator.get(namespace,"SNOMED CT LOINC Collaboration Author"));
            return snomedLoincAuthor;
    }

    /**
     * retrieve a path concept
     * @return TinkarTerm from Concept class
     */
    public static Concept getPathConcept(){
        return TinkarTerm.DEVELOPMENT_PATH;
    }

    /**
     * retrieves identifier concept
     * @return snomedIntId from Concept class
     */
    public static Concept getIdentifierConcept(){
        Concept snomedIntID = Concept.make(PublicIds.of(UuidUtil.fromSNOMED("900000000000294009")));
        return snomedIntID;
    }

    public static Concept getSnomedLoincIdentifierConcept(UUID namespace, String identifierSchemeId) {
        if (identifierSchemeId.equals("705114005")) {
            return Concept.make(PublicIds.of(generateUUID(namespace, "30051010000102")));
        }
        return Concept.make(PublicIds.of(generateUUID(namespace, identifierSchemeId)));
    }

    private static String idToPublicId(UUID namespace, MatchResult id) {
        String idString = id.group();
        String publicIdString = PublicIds.of(generateUUID(namespace, idString)).toString();
        return publicIdString.replaceAll("\"", "");
    }

    private static Pattern getFullIriPattern() {
        // A full IRI is an IRI between "<" and ">"
        // A Prefix declaration is a full IRI
        // An Ontology declaration is a full IRI or an abbreviated IRI
        // but Snomed only uses full IRI, so...
        // <http://www.w3.org/2002/07/owl#>
        // Pattern of characters between less-than and greater-than characters
        return Pattern.compile("<[^>]+>");
    }

    private static String fullIriToPublicId(UUID namespace, MatchResult id) {
        String fullIriString = id.group();
        // the IRI for the public id does not include "<" or ">"
        String iriString = fullIriString.substring(1, fullIriString.length() - 1);
        // Generate UUID from URL bytes
        String publicIdString = PublicIds.of(UuidT5Generator.get(namespace, iriString)).toString();
        return "<" + publicIdString.replaceAll("\"", "") + ">";
    }

    private static Pattern getIdPattern() {
        // Expecting a Snomed OR Loinc identifier following a colon as shown below
        // :609096000 OR :40316-2
        // Pattern of at least one numeric and/or dash character after colon
        return Pattern.compile("(?<=:)([0-9-]+)");
    }

    private static String idToPublicId(MatchResult id) {
        String idString = id.group();
        String publicIdString;
        if (idString.contains("-")) {
            // TODO: Determine namespace to use if this is a Loinc identifier
            idString = "org.loinc." + idString;
            publicIdString = PublicIds.of(UUID.nameUUIDFromBytes(idString.getBytes())).toString();
        } else {
            publicIdString = PublicIds.of(UuidUtil.fromSNOMED(idString)).toString();
        }
        return publicIdString.replaceAll("\"", "");
    }

    private static Pattern getUrlPattern() {
        // Expecting URL formatted as shown below
        // <http://www.w3.org/2002/07/owl#>
        // Pattern of characters between less-than and greater-than characters
        return Pattern.compile("<[^>]+>");
    }

    private static String urlToPublicId(MatchResult id) {
        String urlString = id.group();
        // remove beginning less-than character and ending greater-than character
        String idString = urlString.substring(1, urlString.length()-1);
        // Generate UUID from URL bytes
        String publicIdString = PublicIds.of(UUID.nameUUIDFromBytes(idString.getBytes())).toString();
        return publicIdString.replaceAll("\"", "");
    }

    public static String owlAxiomIdsToPublicIds(UUID namespace, String owlExpression) {
        String publicIdOwlExpression = owlExpression;
        // Replace IRIs with a UUID representation
        Matcher urlMatcher = getFullIriPattern().matcher(publicIdOwlExpression);
        publicIdOwlExpression = urlMatcher.replaceAll(m -> fullIriToPublicId(namespace, m));
        // Replace Snomed identifiers with a UUID representation
        Matcher idMatcher = getIdPattern().matcher(publicIdOwlExpression);
        publicIdOwlExpression = idMatcher.replaceAll(m -> idToPublicId(namespace, m));
        return publicIdOwlExpression;
    }

    public static UUID generateUUID(UUID namespace, String id) {
        return UuidT5Generator.get(namespace, id);
    }
}
