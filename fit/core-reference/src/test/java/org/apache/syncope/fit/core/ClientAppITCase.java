/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.to.client.CASSPTO;
import org.apache.syncope.common.lib.to.client.OIDCRPTO;
import org.apache.syncope.common.lib.to.client.SAML2SPTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class ClientAppITCase extends AbstractITCase {

    @Test
    public void createSAML2SP() {
        createClientApp(ClientAppType.SAML2SP, buildSAML2SP());
    }

    @Test
    public void readSAML2SP() {
        SAML2SPTO samlSpTO = buildSAML2SP();
        samlSpTO = createClientApp(ClientAppType.SAML2SP, samlSpTO);

        SAML2SPTO found = clientAppService.read(ClientAppType.SAML2SP, samlSpTO.getKey());
        assertNotNull(found);
        assertFalse(StringUtils.isBlank(found.getEntityId()));
        assertFalse(StringUtils.isBlank(found.getMetadataLocation()));
        assertTrue(found.isEncryptAssertions());
        assertTrue(found.isEncryptionOptional());
        assertNotNull(found.getRequiredNameIdFormat());
        assertNotNull(found.getAccessPolicy());
        assertNotNull(found.getAuthPolicy());
    }

    @Test
    public void updateSAML2SP() {
        SAML2SPTO samlSpTO = buildSAML2SP();
        samlSpTO = createClientApp(ClientAppType.SAML2SP, samlSpTO);

        AccessPolicyTO accessPolicyTO = new AccessPolicyTO();
        accessPolicyTO.setKey("NewAccessPolicyTest_" + getUUIDString());
        accessPolicyTO.setDescription("New Access policy");
        accessPolicyTO = createPolicy(PolicyType.ACCESS, accessPolicyTO);
        assertNotNull(accessPolicyTO);

        samlSpTO.setEntityId("newEntityId");
        samlSpTO.setAccessPolicy(accessPolicyTO.getKey());

        clientAppService.update(ClientAppType.SAML2SP, samlSpTO);
        SAML2SPTO updated = clientAppService.read(ClientAppType.SAML2SP, samlSpTO.getKey());

        assertNotNull(updated);
        assertEquals("newEntityId", updated.getEntityId());
        assertNotNull(updated.getAccessPolicy());
    }

    @Test
    public void deleteSAML2SP() {
        SAML2SPTO samlSpTO = buildSAML2SP();
        samlSpTO = createClientApp(ClientAppType.SAML2SP, samlSpTO);

        clientAppService.delete(ClientAppType.SAML2SP, samlSpTO.getKey());

        try {
            clientAppService.read(ClientAppType.SAML2SP, samlSpTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void createOIDCRP() {
        createClientApp(ClientAppType.OIDCRP, buildOIDCRP());
    }

    @Test
    public void createCASSP() {
        createClientApp(ClientAppType.CASSP, buildCASSP());
    }

    @Test
    public void readOIDCRP() {
        OIDCRPTO oidcrpTO = buildOIDCRP();
        oidcrpTO = createClientApp(ClientAppType.OIDCRP, oidcrpTO);

        OIDCRPTO found = clientAppService.read(ClientAppType.OIDCRP, oidcrpTO.getKey());
        assertNotNull(found);
        assertFalse(StringUtils.isBlank(found.getClientId()));
        assertFalse(StringUtils.isBlank(found.getClientSecret()));
        assertNotNull(found.getSubjectType());
        assertFalse(found.getSupportedGrantTypes().isEmpty());
        assertFalse(found.getSupportedResponseTypes().isEmpty());
        assertNotNull(found.getAccessPolicy());
        assertNotNull(found.getAuthPolicy());
    }

    @Test
    public void readCASSP() {
        CASSPTO casspTO = buildCASSP();
        casspTO = createClientApp(ClientAppType.CASSP, casspTO);
        CASSPTO found = clientAppService.read(ClientAppType.CASSP, casspTO.getKey());
        assertNotNull(found);
        assertNotNull(found.getServiceId());
        assertNotNull(found.getAccessPolicy());
        assertNotNull(found.getAuthPolicy());
    }

    @Test
    public void updateOIDCRP() {
        OIDCRPTO oidcrpTO = buildOIDCRP();
        oidcrpTO = createClientApp(ClientAppType.OIDCRP, oidcrpTO);

        AccessPolicyTO accessPolicyTO = new AccessPolicyTO();
        accessPolicyTO.setKey("NewAccessPolicyTest_" + getUUIDString());
        accessPolicyTO.setDescription("New Access policy");
        accessPolicyTO = createPolicy(PolicyType.ACCESS, accessPolicyTO);
        assertNotNull(accessPolicyTO);

        oidcrpTO.setClientId("newClientId");
        oidcrpTO.setAccessPolicy(accessPolicyTO.getKey());

        clientAppService.update(ClientAppType.OIDCRP, oidcrpTO);
        OIDCRPTO updated = clientAppService.read(ClientAppType.OIDCRP, oidcrpTO.getKey());

        assertNotNull(updated);
        assertEquals("newClientId", updated.getClientId());
        assertNotNull(updated.getAccessPolicy());
    }

    @Test
    public void delete() {
        OIDCRPTO oidcrpTO = buildOIDCRP();
        oidcrpTO = createClientApp(ClientAppType.OIDCRP, oidcrpTO);

        clientAppService.delete(ClientAppType.OIDCRP, oidcrpTO.getKey());

        try {
            clientAppService.read(ClientAppType.OIDCRP, oidcrpTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void deleteCASSP() {
        CASSPTO casspTO = buildCASSP();
        casspTO = createClientApp(ClientAppType.CASSP, casspTO);

        clientAppService.delete(ClientAppType.CASSP, casspTO.getKey());

        try {
            clientAppService.read(ClientAppType.CASSP, casspTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }
    }

    private CASSPTO buildCASSP() {
        AuthPolicyTO authPolicyTO = new AuthPolicyTO();
        authPolicyTO.setKey("AuthPolicyTest_" + getUUIDString());
        authPolicyTO.setDescription("Authentication Policy");
        authPolicyTO = createPolicy(PolicyType.AUTH, authPolicyTO);
        assertNotNull(authPolicyTO);

        AccessPolicyTO accessPolicyTO = new AccessPolicyTO();
        accessPolicyTO.setKey("AccessPolicyTest_" + getUUIDString());
        accessPolicyTO.setDescription("Access policy");
        accessPolicyTO = createPolicy(PolicyType.ACCESS, accessPolicyTO);
        assertNotNull(accessPolicyTO);

        CASSPTO casspTO = new CASSPTO();
        casspTO.setName("ExampleRP_" + getUUIDString());
        casspTO.setClientAppId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        casspTO.setDescription("Example OIDC RP application");
        casspTO.setServiceId("https://cassp.example.org/" + UUID.randomUUID().getMostSignificantBits());

        casspTO.setAuthPolicy(authPolicyTO.getKey());
        casspTO.setAccessPolicy(accessPolicyTO.getKey());
        return casspTO;
    }


}
