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
package org.apache.syncope.wa.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.policy.AllowedAttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyCriteriaConf;
import org.apache.syncope.common.lib.to.client.OIDCRPTO;
import org.apache.syncope.common.lib.to.client.SAML2SPTO;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apache.syncope.common.rest.api.service.wa.WAClientAppService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.services.AnyAuthenticationHandlerRegisteredServiceAuthenticationPolicyCriteria;
import org.apereo.cas.services.DenyAllAttributeReleasePolicy;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ReturnAllowedAttributeReleasePolicy;
import org.apereo.cas.services.ReturnMappedAttributeReleasePolicy;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.apereo.cas.util.RandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class SyncopeWAServiceRegistryTest extends AbstractTest {

    @Autowired
    private WARestClient wARestClient;

    @Autowired
    private ServicesManager servicesManager;

    private static OIDCRPTO buildOIDCRP() {
        OIDCRPTO oidcrpTO = new OIDCRPTO();
        oidcrpTO.setName("ExampleRP_" + getUUIDString());
        oidcrpTO.setClientAppId(RandomUtils.nextLong());
        oidcrpTO.setDescription("Example OIDC RP application");
        oidcrpTO.setClientId("clientId_" + getUUIDString());
        oidcrpTO.setClientSecret("secret");
        oidcrpTO.getRedirectUris().addAll(List.of("uri1", "uri2"));
        oidcrpTO.setSubjectType(OIDCSubjectType.PUBLIC);
        oidcrpTO.getSupportedGrantTypes().add("something");
        oidcrpTO.getSupportedResponseTypes().add("something");

        return oidcrpTO;
    }

    protected SAML2SPTO buildSAML2SP() {
        SAML2SPTO saml2spto = new SAML2SPTO();
        saml2spto.setName("ExampleSAML2SP_" + getUUIDString());
        saml2spto.setClientAppId(RandomUtils.nextLong());
        saml2spto.setDescription("Example SAML 2.0 service provider");
        saml2spto.setEntityId("SAML2SPEntityId_" + getUUIDString());
        saml2spto.setMetadataLocation("file:./test.xml");
        saml2spto.setRequiredNameIdFormat(SAML2SPNameId.EMAIL_ADDRESS);
        saml2spto.setEncryptionOptional(true);
        saml2spto.setEncryptAssertions(true);

        return saml2spto;
    }

    private static void addAttributes(final boolean withReleaseAttributes,
            final boolean withAttrReleasePolicy,
            final WAClientApp waClientApp) {
        DefaultAuthPolicyConf authPolicyConf = new DefaultAuthPolicyConf();
        DefaultAuthPolicyCriteriaConf criteria = new DefaultAuthPolicyCriteriaConf();
        criteria.setAll(true);
        authPolicyConf.setCriteria(criteria);
        authPolicyConf.getAuthModules().add("TestAuthModule");

        waClientApp.setAuthPolicyConf(authPolicyConf);

        if (withReleaseAttributes) {
            Map<String, Object> releaseAttrs;
            releaseAttrs = new HashMap<>();
            releaseAttrs.put("uid", "username");
            releaseAttrs.put("cn", "fullname");
            waClientApp.getReleaseAttrs().putAll(releaseAttrs);
        }

        DefaultAccessPolicyConf accessPolicyConf = new DefaultAccessPolicyConf();
        accessPolicyConf.setEnabled(true);
        accessPolicyConf.addRequiredAttr("cn", Set.of("admin", "Admin", "TheAdmin"));
        waClientApp.setAccessPolicyConf(accessPolicyConf);

        if (withAttrReleasePolicy) {
            AllowedAttrReleasePolicyConf attrReleasePolicyConf = new AllowedAttrReleasePolicyConf();
            attrReleasePolicyConf.getAllowedAttrs().add("cn");
            waClientApp.setAttrReleasePolicyConf(attrReleasePolicyConf);
        }
    }

    @Test
    public void addClientApp() {
        // 1. start with no client apps defined on mocked Core
        SyncopeClient syncopeClient = wARestClient.getSyncopeClient();
        assertNotNull(syncopeClient);

        SyncopeCoreTestingServer.APPS.clear();

        WAClientAppService service = syncopeClient.getService(WAClientAppService.class);
        assertTrue(service.list().isEmpty());

        // 2. add one client app on mocked Core, nothing on WA yet
        WAClientApp waClientApp = new WAClientApp();
        waClientApp.setClientAppTO(buildOIDCRP());
        Long clientAppId = waClientApp.getClientAppTO().getClientAppId();
        addAttributes(true, true, waClientApp);
        
        SyncopeCoreTestingServer.APPS.add(waClientApp);
        List<WAClientApp> apps = service.list();
        assertEquals(1, apps.size());

        assertNotNull(servicesManager.findServiceBy(clientAppId));

        // 3. trigger client app refresh
        Collection<RegisteredService> load = servicesManager.load();
        assertEquals(3, load.size());

        // 4. look for the service created above
        RegisteredService found = servicesManager.findServiceBy(clientAppId);
        assertNotNull(found);
        assertTrue(found instanceof OidcRegisteredService);
        OidcRegisteredService oidcRegisteredService = OidcRegisteredService.class.cast(found);
        OIDCRPTO oidcrpto = OIDCRPTO.class.cast(waClientApp.getClientAppTO());
        assertEquals("uri1|uri2", oidcRegisteredService.getServiceId());
        assertEquals(oidcrpto.getClientId(), oidcRegisteredService.getClientId());
        assertEquals(oidcrpto.getClientSecret(), oidcRegisteredService.getClientSecret());
        assertTrue(oidcRegisteredService.getAuthenticationPolicy().getRequiredAuthenticationHandlers().
                contains("TestAuthModule"));
        assertTrue(((AnyAuthenticationHandlerRegisteredServiceAuthenticationPolicyCriteria) oidcRegisteredService.
                getAuthenticationPolicy().getCriteria()).isTryAll());
        assertTrue(oidcRegisteredService.getAttributeReleasePolicy() instanceof ReturnMappedAttributeReleasePolicy);
        assertFalse(oidcRegisteredService.getAttributeReleasePolicy() instanceof ReturnAllowedAttributeReleasePolicy);
        assertFalse(oidcRegisteredService.getAttributeReleasePolicy() instanceof DenyAllAttributeReleasePolicy);

        // 5. more client with different attributes 
        waClientApp = new WAClientApp();
        waClientApp.setClientAppTO(buildSAML2SP());
        clientAppId = waClientApp.getClientAppTO().getClientAppId();
        addAttributes(false, true, waClientApp);

        SyncopeCoreTestingServer.APPS.add(waClientApp);
        apps = service.list();
        assertEquals(2, apps.size());

        load = servicesManager.load();
        assertEquals(4, load.size());

        found = servicesManager.findServiceBy(clientAppId);
        assertTrue(found instanceof SamlRegisteredService);
        SamlRegisteredService samlRegisteredService = SamlRegisteredService.class.cast(found);
        SAML2SPTO samlspto = SAML2SPTO.class.cast(waClientApp.getClientAppTO());
        assertEquals(samlspto.getMetadataLocation(), samlRegisteredService.getMetadataLocation());
        assertEquals(samlspto.getEntityId(), samlRegisteredService.getServiceId());
        assertTrue(samlRegisteredService.getAuthenticationPolicy().getRequiredAuthenticationHandlers().
                contains("TestAuthModule"));
        assertNotNull(found.getAccessStrategy());
        assertFalse(samlRegisteredService.getAttributeReleasePolicy() instanceof ReturnMappedAttributeReleasePolicy);
        assertTrue(samlRegisteredService.getAttributeReleasePolicy() instanceof ReturnAllowedAttributeReleasePolicy);
        assertFalse(samlRegisteredService.getAttributeReleasePolicy() instanceof DenyAllAttributeReleasePolicy);

        waClientApp = new WAClientApp();
        waClientApp.setClientAppTO(buildSAML2SP());
        clientAppId = waClientApp.getClientAppTO().getClientAppId();
        addAttributes(false, false, waClientApp);

        SyncopeCoreTestingServer.APPS.add(waClientApp);
        apps = service.list();
        assertEquals(3, apps.size());

        load = servicesManager.load();
        assertEquals(5, load.size());

        found = servicesManager.findServiceBy(clientAppId);
        assertFalse(found.getAttributeReleasePolicy() instanceof ReturnMappedAttributeReleasePolicy);
        assertFalse(found.getAttributeReleasePolicy() instanceof ReturnAllowedAttributeReleasePolicy);
        assertTrue(found.getAttributeReleasePolicy() instanceof DenyAllAttributeReleasePolicy);

    }
}
