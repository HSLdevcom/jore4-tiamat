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

package org.rutebanken.tiamat.rest.graphql.types;

import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import org.rutebanken.tiamat.rest.graphql.fetchers.GroupOfStopPlacesMembersFetcher;
import org.rutebanken.tiamat.rest.graphql.fetchers.GroupOfStopPlacesPurposeOfGroupingFetcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.DESCRIPTION;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.GROUP_OF_STOP_PLACES_MEMBERS;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.NAME;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.OUTPUT_TYPE_GROUP_OF_STOPPLACES;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.PURPOSE_OF_GROUPING;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.SHORT_NAME;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.VALID_BETWEEN;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.VERSION;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.VERSION_COMMENT;
import static org.rutebanken.tiamat.rest.graphql.types.CustomGraphQLTypes.embeddableMultilingualStringObjectType;
import static org.rutebanken.tiamat.rest.graphql.types.CustomGraphQLTypes.geometryFieldDefinition;
import static org.rutebanken.tiamat.rest.graphql.types.CustomGraphQLTypes.netexIdFieldDefinition;

@Component
public class GroupOfStopPlacesObjectTypeCreator {

    @Autowired
    private GroupOfStopPlacesMembersFetcher groupOfStopPlacesMembersFetcher;

    @Autowired
    private GroupOfStopPlacesPurposeOfGroupingFetcher groupOfStopPlacesPurposeOfGroupingFetcher;

    public GraphQLObjectType create(GraphQLInterfaceType stopPlaceInterface,
                                    GraphQLObjectType purposeOfGroupingType,
                                    GraphQLObjectType validBetweenObjectType) {

        return newObject()
                .name(OUTPUT_TYPE_GROUP_OF_STOPPLACES)
                .field(netexIdFieldDefinition)
                .field(newFieldDefinition()
                        .name(NAME)
                        .type(embeddableMultilingualStringObjectType))
                .field(newFieldDefinition()
                        .name(SHORT_NAME)
                        .type(embeddableMultilingualStringObjectType))
                .field(newFieldDefinition()
                        .name(DESCRIPTION)
                        .type(embeddableMultilingualStringObjectType))
                .field(newFieldDefinition()
                        .name(VERSION)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(VERSION_COMMENT)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(PURPOSE_OF_GROUPING)
                        .type(purposeOfGroupingType)
                        .dataFetcher(groupOfStopPlacesPurposeOfGroupingFetcher))
                .field(newFieldDefinition()
                        .name(VALID_BETWEEN)
                        .type(validBetweenObjectType))
                .field(geometryFieldDefinition)
                .field(newFieldDefinition()
                        .name(GROUP_OF_STOP_PLACES_MEMBERS)
                        .type(new GraphQLList(stopPlaceInterface))
                        .dataFetcher(groupOfStopPlacesMembersFetcher))
                        .build();
    }
}
