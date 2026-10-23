/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.rest.graphql.scalars;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.LOCAL_DATE_SCALAR_DESCRIPTION;

@Component
public class LocalDateScalar {

    public static final String EXAMPLE_DATE = "2025-04-23";

    public static final String DATE_PATTERN = "yyyy-MM-dd";

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    private GraphQLScalarType graphQLLocalDateScalar;

    public GraphQLScalarType getGraphQLLocalDateScalar() {
        if (graphQLLocalDateScalar == null) {
            graphQLLocalDateScalar = createGraphQLocalDateScalar();
        }

        return graphQLLocalDateScalar;
    }

    private GraphQLScalarType createGraphQLocalDateScalar() {
        return new GraphQLScalarType.Builder()
                .name("LocalDate")
                .description(LOCAL_DATE_SCALAR_DESCRIPTION)
                .coercing(new Coercing<LocalDate, String>() {
                    @Override
                    public String serialize(Object input, GraphQLContext graphQLContext, Locale locale) {
                        if (input instanceof LocalDate localDate) {
                            return localDate.format(DATE_FORMATTER);
                        }

                        // If the input is not a LocalDate, we can try to parse it as a string
                        // and then format it, or return null if it's not a valid date string.
                        if (input instanceof String inputString) {
                            try {
                                return LocalDate.parse(inputString, DATE_FORMATTER).format(DATE_FORMATTER);
                            } catch (DateTimeParseException e) {
                                return null; // Invalid date string
                            }
                        }
                        return null;
                    }

                    @Override
                    public LocalDate parseValue(Object input, GraphQLContext graphQLContext, Locale locale) {
                        if (input instanceof String stringValue) {
                            try {
                                return LocalDate.parse(stringValue, DATE_FORMATTER);
                            } catch (DateTimeParseException e) {
                                throw new RuntimeException("Invalid date format. Expected yyyy-MM-dd.", e);
                            }
                        }
                        return null;
                    }

                    @Override
                    public LocalDate parseLiteral(Value input, CoercedVariables variables, GraphQLContext graphQLContext, Locale locale) {
                        if (input instanceof StringValue stringValue) {
                            return parseValue(stringValue.getValue(), graphQLContext, locale);
                        }

                        return null;
                    }
                }).build();
    }
}
