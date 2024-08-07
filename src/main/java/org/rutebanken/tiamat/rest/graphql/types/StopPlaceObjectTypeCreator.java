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

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import org.rutebanken.tiamat.rest.graphql.scalars.TransportModeScalar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.ADJACENT_SITES;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.ADJACENT_SITES_DESCRIPTION;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.ORGANISATIONS;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.OUTPUT_TYPE_STOPPLACE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.PARENT_SITE_REF;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.QUAYS;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.STOP_PLACE_TYPE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.WEIGHTING;
import static org.rutebanken.tiamat.rest.graphql.types.CustomGraphQLTypes.interchangeWeightingEnum;
import static org.rutebanken.tiamat.rest.graphql.types.CustomGraphQLTypes.stopPlaceTypeEnum;
import static org.rutebanken.tiamat.rest.graphql.types.VersionLessEntityRef.versionLessEntityRef;

@Component
public class StopPlaceObjectTypeCreator {

    @Autowired
    private TransportModeScalar transportModeScalar;

    public GraphQLObjectType create(
            GraphQLInterfaceType stopPlaceInterface,
            List<GraphQLFieldDefinition> stopPlaceInterfaceFields,
            List<GraphQLFieldDefinition> commonFieldsList,
            GraphQLObjectType quayObjectType,
            GraphQLObjectType stopPlaceOrganisationRefObjectType
    ) {
        return newObject()
                .name(OUTPUT_TYPE_STOPPLACE)
                .withInterface(stopPlaceInterface)
                .fields(stopPlaceInterfaceFields)
                .fields(commonFieldsList)
                .fields(transportModeScalar.getTransportModeFieldsList())
                .field(newFieldDefinition()
                        .name(STOP_PLACE_TYPE)
                        .type(stopPlaceTypeEnum))
                .field(newFieldDefinition()
                        .name(WEIGHTING)
                        .type(interchangeWeightingEnum))
                .field(newFieldDefinition()
                        .name(PARENT_SITE_REF)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(ADJACENT_SITES)
                        .type(new GraphQLList(versionLessEntityRef))
                        .description(ADJACENT_SITES_DESCRIPTION))
                .field(newFieldDefinition()
                        .name(ORGANISATIONS)
                        .type(new GraphQLList(stopPlaceOrganisationRefObjectType)))
                .field(newFieldDefinition()
                        .name(QUAYS)
                        .type(new GraphQLList(quayObjectType)))
                .build();
    }
}
