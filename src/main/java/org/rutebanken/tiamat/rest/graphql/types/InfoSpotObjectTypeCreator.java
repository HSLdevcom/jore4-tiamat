package org.rutebanken.tiamat.rest.graphql.types;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import org.rutebanken.tiamat.model.DisplayTypeEnumeration;
import org.rutebanken.tiamat.model.PosterPlaceTypeEnumeration;
import org.rutebanken.tiamat.model.PosterSizeEnumeration;
import org.springframework.stereotype.Component;

import static graphql.Scalars.GraphQLBigInteger;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.BACKLIGHT;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.DESCRIPTION;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.DISPLAY_TYPE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.FLOOR;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.ID;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.INFO_SPOTS;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.INPUT_TYPE_INFO_SPOT;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.LABEL;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.LINES;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.MAINTENANCE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.MAXIMUM_STAY;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.ON_STOP_PLACE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.OUTPUT_TYPE_INFO_SPOT;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.POSTER;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.POSTER_PLACE_SIZE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.POSTER_PLACE_TYPE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.POSTER_SIZE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.POSTER_TYPE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.PURPOSE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.RAIL_INFORMATION;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.SPEECH_PROPERTY;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.VALID_BETWEEN;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.VERSION;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.ZONE_LABEL;
import static org.rutebanken.tiamat.rest.graphql.types.CustomGraphQLTypes.createCustomEnumType;
import static org.rutebanken.tiamat.rest.graphql.types.CustomGraphQLTypes.geometryFieldDefinition;
import static org.rutebanken.tiamat.rest.graphql.types.CustomGraphQLTypes.netexIdFieldDefinition;

@Component
public class InfoSpotObjectTypeCreator {


    public static GraphQLEnumType posterPlaceTypeEnum = createCustomEnumType(POSTER_PLACE_TYPE, PosterPlaceTypeEnumeration.class);
    public static GraphQLEnumType posterSizeEnum = createCustomEnumType(POSTER_PLACE_SIZE, PosterSizeEnumeration.class);
    public static GraphQLEnumType displayTypeEnum = createCustomEnumType(DISPLAY_TYPE, DisplayTypeEnumeration.class);

    public static GraphQLObjectType posterObjectType =
            newObject()
                    .name(POSTER)
                    .field(newFieldDefinition()
                            .name(LABEL)
                            .type(GraphQLString))
                    .field(newFieldDefinition()
                            .name(POSTER_TYPE)
                            .type(GraphQLString))
                    .field(newFieldDefinition()
                            .name(POSTER_SIZE)
                            .type(posterSizeEnum))
                    .field(newFieldDefinition()
                            .name(LINES)
                            .type(GraphQLString))
                    .build();

    public static GraphQLInputObjectType posterInputObjectType =
            newInputObject()
                    .name(POSTER)
                    .field(newInputObjectField()
                            .name(LABEL)
                            .type(GraphQLString))
                    .field(newInputObjectField()
                            .name(POSTER_TYPE)
                            .type(GraphQLString))
                    .field(newInputObjectField()
                            .name(POSTER_SIZE)
                            .type(posterSizeEnum))
                    .field(newInputObjectField()
                            .name(LINES)
                            .type(GraphQLString))
                    .build();

    public GraphQLObjectType createObjectType(GraphQLInterfaceType stopPlaceInterface, GraphQLObjectType validBetweenObjectType) {
        return newObject()
                .name(OUTPUT_TYPE_INFO_SPOT)
                .field(netexIdFieldDefinition)
                .field(newFieldDefinition()
                        .name(VERSION)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(VALID_BETWEEN)
                        .type(validBetweenObjectType))
                .field(geometryFieldDefinition)
                .field(newFieldDefinition()
                        .name(LABEL)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(PURPOSE)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(POSTER_PLACE_TYPE)
                        .type(posterPlaceTypeEnum))
                .field(newFieldDefinition()
                        .name(POSTER_PLACE_SIZE)
                        .type(posterSizeEnum))
                .field(newFieldDefinition()
                        .name(DESCRIPTION)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(BACKLIGHT)
                        .type(GraphQLBoolean))
                .field(newFieldDefinition()
                        .name(MAINTENANCE)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(ZONE_LABEL)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(RAIL_INFORMATION)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(FLOOR)
                        .type(GraphQLString))
                .field(newFieldDefinition()
                        .name(SPEECH_PROPERTY)
                        .type(GraphQLBoolean))
                .field(newFieldDefinition()
                        .name(DISPLAY_TYPE)
                        .type(displayTypeEnum))
//                .field(newFieldDefinition()
//                        .name(POSTER)
//                        .type(new GraphQLList(posterObjectType)))
//                .field(newFieldDefinition()
//                        .name(ON_STOP_PLACE)
//                        .type(new GraphQLList(GraphQLString)))
                .build();
    }


    public GraphQLInputObjectType createInputObjectType(GraphQLInputObjectType validBetweenInputObjectType) {
        return newInputObject()
                .name(INPUT_TYPE_INFO_SPOT)
                .field(newInputObjectField()
                        .name(ID)
                        .type(GraphQLString))
                .field(newInputObjectField()
                        .name(LABEL)
                        .type(GraphQLString))
                .field(newInputObjectField()
                        .name(PURPOSE)
                        .type(GraphQLString))
                .field(newInputObjectField()
                        .name(POSTER_PLACE_TYPE)
                        .type(posterPlaceTypeEnum))
                .field(newInputObjectField()
                        .name(POSTER_PLACE_SIZE)
                        .type(posterSizeEnum))
                .field(newInputObjectField()
                        .name(DESCRIPTION)
                        .type(GraphQLString))
                .field(newInputObjectField()
                        .name(BACKLIGHT)
                        .type(GraphQLBoolean))
                .field(newInputObjectField()
                        .name(MAINTENANCE)
                        .type(GraphQLString))
                .field(newInputObjectField()
                        .name(ZONE_LABEL)
                        .type(GraphQLString))
                .field(newInputObjectField()
                        .name(RAIL_INFORMATION)
                        .type(GraphQLString))
                .field(newInputObjectField()
                        .name(FLOOR)
                        .type(GraphQLString))
                .field(newInputObjectField()
                        .name(SPEECH_PROPERTY)
                        .type(GraphQLBoolean))
                .field(newInputObjectField()
                        .name(DISPLAY_TYPE)
                        .type(displayTypeEnum))
//                .field(newInputObjectField()
//                        .name(POSTER)
//                        .type(new GraphQLList(posterObjectType)))
//                .field(newInputObjectField()
//                        .name(ON_STOP_PLACE)
//                        .type(new GraphQLList(GraphQLString)))
                .build();

    }
}
