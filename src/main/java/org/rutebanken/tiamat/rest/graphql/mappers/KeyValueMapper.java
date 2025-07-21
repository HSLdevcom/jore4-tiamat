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

import org.rutebanken.tiamat.model.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyValueMapper {
    public static Map<String, Value> getKeyValuesMap(List<Map<String, Object>> keyValuesInput) {
        if (keyValuesInput == null) {
            return null;
        }

        Map<String, Value> keyValues = new HashMap<>();

        if (keyValuesInput != null) {
            keyValuesInput.forEach(kvInput -> {
                var key = (String) kvInput.get("key");

                if (key != null && !key.isBlank()) {
                    var values = kvInput.get("values");

                    if (values != null) {
                        keyValues.put(key, new Value((List<String>) values));
                    } else {
                        keyValues.put(key, null);
                    }
                }
            });
        }

        return keyValues;
    }
}
