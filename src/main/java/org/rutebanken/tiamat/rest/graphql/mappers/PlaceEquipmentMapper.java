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

package org.rutebanken.tiamat.rest.graphql.mappers;

import org.rutebanken.tiamat.model.CycleStorageEnumeration;
import org.rutebanken.tiamat.model.CycleStorageEquipment;
import org.rutebanken.tiamat.model.GenderLimitationEnumeration;
import org.rutebanken.tiamat.model.GeneralSign;
import org.rutebanken.tiamat.model.PlaceEquipment;
import org.rutebanken.tiamat.model.SanitaryEquipment;
import org.rutebanken.tiamat.model.ShelterEquipment;
import org.rutebanken.tiamat.model.SignContentEnumeration;
import org.rutebanken.tiamat.model.TicketingEquipment;
import org.rutebanken.tiamat.model.WaitingRoomEquipment;
import org.rutebanken.tiamat.model.hsl.ElectricityTypeEnumeration;
import org.rutebanken.tiamat.model.hsl.ShelterTypeEnumeration;
import org.rutebanken.tiamat.model.hsl.ShelterConditionEnumeration;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.BICYCLE_PARKING;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.CONTENT;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.CYCLE_STORAGE_EQUIPMENT;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.CYCLE_STORAGE_TYPE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.ENCLOSED;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.GENDER;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.GENERAL_SIGN;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.HEATED;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.LEANING_RAIL;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.LINE_SIGNAGE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.MAIN_LINE_SIGN;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.NOTE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.NUMBER_OF_FRAMES;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.NUMBER_OF_MACHINES;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.NUMBER_OF_SPACES;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.NUMBER_OF_TOILETS;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.OUTSIDE_BENCH;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.PLACE_EQUIPMENTS;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.PRIVATE_CODE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.REPLACES_RAIL_SIGN;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.SANITARY_EQUIPMENT;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.SEATS;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.SHELTER_CONDITION;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.SHELTER_ELECTRICITY;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.SHELTER_EQUIPMENT;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.SHELTER_FASCIA_BOARD_TAPING;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.SHELTER_HAS_DISPLAY;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.SHELTER_LIGHTING;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.SHELTER_TYPE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.SIGN_CONTENT_TYPE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.STEP_FREE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.TICKETING_EQUIPMENT;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.TICKET_MACHINES;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.TICKET_OFFICE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.TIMETABLE_CABINETS;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.TRASH_CAN;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.WAITING_ROOM_EQUIPMENT;
import static org.rutebanken.tiamat.rest.graphql.mappers.EmbeddableMultilingualStringMapper.getEmbeddableString;
import static org.rutebanken.tiamat.rest.graphql.mappers.PrivateCodeMapper.getPrivateCodeStructure;

@Component(value = "GraphQLPlaceEquipmentMapper")
public class PlaceEquipmentMapper {

    public Optional<PlaceEquipment> map(Map input) {
        if (input.get(PLACE_EQUIPMENTS) != null) {
            PlaceEquipment equipments = new PlaceEquipment();

            Map<String, Object> equipmentInput = (Map) input.get(PLACE_EQUIPMENTS);

            if (equipmentInput.get(SANITARY_EQUIPMENT) != null) {

                List equipment = (List) equipmentInput.get(SANITARY_EQUIPMENT);
                for (Object item : equipment) {
                    Map<String, Object> sanitaryEquipment = (Map<String, Object>) item;

                    SanitaryEquipment toalett = new SanitaryEquipment();
                    toalett.setNumberOfToilets((BigInteger) sanitaryEquipment.get(NUMBER_OF_TOILETS));
                    toalett.setGender((GenderLimitationEnumeration) sanitaryEquipment.get(GENDER));
                    equipments.getInstalledEquipment().add(toalett);
                }
            }

            if (equipmentInput.get(SHELTER_EQUIPMENT) != null) {

                List equipment = (List) equipmentInput.get(SHELTER_EQUIPMENT);
                for (Object item : equipment) {
                    Map<String, Object> shelterEquipment = (Map<String, Object>) item;
                    ShelterEquipment leskur = new ShelterEquipment();
                    leskur.setEnclosed((Boolean) shelterEquipment.get(ENCLOSED));
                    leskur.setSeats((BigInteger) shelterEquipment.get(SEATS));
                    leskur.setStepFree((Boolean) shelterEquipment.get(STEP_FREE));
                    leskur.setShelterType((ShelterTypeEnumeration) shelterEquipment.get(SHELTER_TYPE));
                    leskur.setShelterElectricity((ElectricityTypeEnumeration) shelterEquipment.get(SHELTER_ELECTRICITY));
                    leskur.setShelterLighting((Boolean) shelterEquipment.get(SHELTER_LIGHTING));
                    leskur.setShelterCondition((ShelterConditionEnumeration) shelterEquipment.get(SHELTER_CONDITION));
                    leskur.setTimetableCabinets((Integer) shelterEquipment.get(TIMETABLE_CABINETS));
                    leskur.setTrashCan((Boolean) shelterEquipment.get(TRASH_CAN));
                    leskur.setShelterHasDisplay((Boolean) shelterEquipment.get(SHELTER_HAS_DISPLAY));
                    leskur.setBicycleParking((Boolean) shelterEquipment.get(BICYCLE_PARKING));
                    leskur.setLeaningRail((Boolean) shelterEquipment.get(LEANING_RAIL));
                    leskur.setOutsideBench((Boolean) shelterEquipment.get(OUTSIDE_BENCH));
                    leskur.setShelterFasciaBoardTaping((Boolean) shelterEquipment.get(SHELTER_FASCIA_BOARD_TAPING));
                    equipments.getInstalledEquipment().add(leskur);
                }
            }

            if (equipmentInput.get(CYCLE_STORAGE_EQUIPMENT) != null) {

                List equipment = (List) equipmentInput.get(CYCLE_STORAGE_EQUIPMENT);
                for (Object item : equipment) {
                    Map<String, Object> cycleStorageEquipment = (Map<String, Object>) item;
                    CycleStorageEquipment sykkelskur = new CycleStorageEquipment();
                    sykkelskur.setNumberOfSpaces((BigInteger) cycleStorageEquipment.get(NUMBER_OF_SPACES));
                    sykkelskur.setCycleStorageType((CycleStorageEnumeration) cycleStorageEquipment.get(CYCLE_STORAGE_TYPE));
                    equipments.getInstalledEquipment().add(sykkelskur);
                }
            }

            if (equipmentInput.get(WAITING_ROOM_EQUIPMENT) != null) {

                List equipment = (List) equipmentInput.get(WAITING_ROOM_EQUIPMENT);
                for (Object item : equipment) {
                    Map<String, Object> waitingRoomEquipment = (Map<String, Object>) item;
                    WaitingRoomEquipment venterom = new WaitingRoomEquipment();
                    venterom.setSeats((BigInteger) waitingRoomEquipment.get(SEATS));
                    venterom.setHeated((Boolean) waitingRoomEquipment.get(HEATED));
                    venterom.setStepFree((Boolean) waitingRoomEquipment.get(STEP_FREE));
                    equipments.getInstalledEquipment().add(venterom);
                }
            }

            if (equipmentInput.get(TICKETING_EQUIPMENT) != null) {

                List equipment = (List) equipmentInput.get(TICKETING_EQUIPMENT);
                for (Object item : equipment) {
                    Map<String, Object> ticketingEquipment = (Map<String, Object>) item;
                    TicketingEquipment billettAutomat = new TicketingEquipment();
                    billettAutomat.setTicketOffice((Boolean) ticketingEquipment.get(TICKET_OFFICE));
                    billettAutomat.setTicketMachines((Boolean) ticketingEquipment.get(TICKET_MACHINES));
                    billettAutomat.setNumberOfMachines((BigInteger) ticketingEquipment.get(NUMBER_OF_MACHINES));
                    equipments.getInstalledEquipment().add(billettAutomat);
                }
            }

            if (equipmentInput.get(GENERAL_SIGN) != null) {

                List equipment = (List) equipmentInput.get(GENERAL_SIGN);
                for (Object item : equipment) {

                    Map<String, Object> generalSignEquipment = (Map<String, Object>) item;

                    GeneralSign skilt = new GeneralSign();
                    skilt.setPrivateCode(getPrivateCodeStructure((Map) generalSignEquipment.get(PRIVATE_CODE)));
                    skilt.setContent(getEmbeddableString((Map) generalSignEquipment.get(CONTENT)));
                    skilt.setSignContentType((SignContentEnumeration) generalSignEquipment.get(SIGN_CONTENT_TYPE));
                    skilt.setLineSignage((Boolean) generalSignEquipment.get(LINE_SIGNAGE));
                    skilt.setReplacesRailSign((Boolean) generalSignEquipment.get(REPLACES_RAIL_SIGN));
                    skilt.setMainLineSign((Boolean) generalSignEquipment.get(MAIN_LINE_SIGN));
                    skilt.setNumberOfFrames((Integer) generalSignEquipment.get(NUMBER_OF_FRAMES));
                    skilt.setNote(getEmbeddableString((Map) generalSignEquipment.get(NOTE)));
                    equipments.getInstalledEquipment().add(skilt);
                }
            }
            return Optional.of(equipments);
        }
        return Optional.empty();
    }
}
