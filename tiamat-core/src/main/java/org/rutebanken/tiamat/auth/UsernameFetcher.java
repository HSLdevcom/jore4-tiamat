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

package org.rutebanken.tiamat.auth;


import org.rutebanken.helper.organisation.user.UserInfoExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;

@Service
public class UsernameFetcher {

    private final UserInfoExtractor userInfoExtractor;
    private final HttpServletRequest request;

    public UsernameFetcher(UserInfoExtractor userInfoExtractor) {
        this(userInfoExtractor, null);
    }

    @Autowired
    public UsernameFetcher(UserInfoExtractor userInfoExtractor, HttpServletRequest request) {
        this.userInfoExtractor = userInfoExtractor;
        this.request = request;
    }

    /**
     * Return the request user ID, falling back to the authenticated preferred username.
     */
    @Nullable
    public String getUserNameForAuthenticatedUser() {
        try {
            String userId = request.getHeader("X-Hasura-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return userId;
            }
        } catch (Exception e) {
            // Ignore and fallback to Spring Security
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null
                || authentication.getPrincipal() == null
                || !(authentication.getPrincipal() instanceof Jwt)) {
            return null;
        }
        return userInfoExtractor.getPreferredName();
    }
}
