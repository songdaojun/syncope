/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.propagation;

import java.util.Set;
import javassist.NotFoundException;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.client.to.PropertyTO;
import org.syncope.core.persistence.ConnectorInstanceLoader;
import org.syncope.core.persistence.beans.ConnectorInstance;

/**
 * Intercept calls to ConnectorFacade's methods and check if the correspondant
 * connector instance has been configured to allow every single operation:
 * if not, simply do nothig.
 */
public class ConnectorFacadeProxy {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            ConnectorFacadeProxy.class);
    /**
     * Connector facade wrapped instance.
     */
    private final ConnectorFacade connector;
    /**
     * Set of configure connecto instance capabilities.
     * @see org.syncope.core.persistence.beans.ConnectorInstance
     */
    private final Set<ConnectorInstance.Capabitily> capabitilies;

    /**
     * Use the passed connector instance to build a ConnectorFacade that will
     * be used to make all wrapped calls.
     *
     * @param connectorInstance the connector instance configuration
     * @throws NotFoundException when not able to fetch all the required data
     * @see ConnectorKey
     * @see ConnectorInfo
     * @see APIConfiguration
     * @see ConfigurationProperties
     * @see ConnectorFacade
     */
    public ConnectorFacadeProxy(final ConnectorInstance connectorInstance)
            throws NotFoundException {

        // specify a connector.
        ConnectorKey key = new ConnectorKey(
                connectorInstance.getBundleName(),
                connectorInstance.getVersion(),
                connectorInstance.getConnectorName());

        if (key == null) {
            throw new NotFoundException("Connector Key");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("\nBundle name: " + key.getBundleName()
                    + "\nBundle version: " + key.getBundleVersion()
                    + "\nBundle class: " + key.getConnectorName());
        }

        // get the specified connector.
        ConnectorInfo info =
                ConnectorInstanceLoader.getConnectorManager().findConnectorInfo(
                key);

        if (info == null) {
            throw new NotFoundException("Connector Info");
        }

        // create default configuration
        APIConfiguration apiConfig = info.createDefaultAPIConfiguration();

        if (apiConfig == null) {
            throw new NotFoundException("Default API configuration");
        }

        // retrieve the ConfigurationProperties.
        ConfigurationProperties properties =
                apiConfig.getConfigurationProperties();

        if (properties == null) {
            throw new NotFoundException("Configuration properties");
        }

        // Print out what the properties are (not necessary)
        if (LOG.isDebugEnabled()) {
            for (String propName : properties.getPropertyNames()) {
                LOG.debug("\nProperty Name: "
                        + properties.getProperty(propName).getName()
                        + "\nProperty Type: "
                        + properties.getProperty(propName).getType());
            }
        }

        // Set all of the ConfigurationProperties needed by the connector.
        Set<PropertyTO> configuration =
                (Set<PropertyTO>) ConnectorInstanceLoader.buildFromXML(
                connectorInstance.getXmlConfiguration());
        for (PropertyTO property : configuration) {
            properties.setPropertyValue(
                    property.getKey(), property.getValue());
        }

        // Use the ConnectorFacadeFactory's newInstance() method to get
        // a new connector.
        ConnectorFacade connector =
                ConnectorFacadeFactory.getInstance().newInstance(apiConfig);

        if (connector == null) {
            throw new NotFoundException("Connector");
        }

        // Make sure we have set up the Configuration properly
        connector.validate();
        //connector.test(); //needs a target resource deployed

        this.connector = connector;
        this.capabitilies = connectorInstance.getCapabilities();
    }

    public Uid create(final PropagationManager.PropagationMode propagationMode,
            final ObjectClass oclass,
            final Set<Attribute> attrs,
            final OperationOptions options) {

        Uid result = null;

        if (propagationMode == PropagationManager.PropagationMode.SYNC
                ? capabitilies.contains(
                ConnectorInstance.Capabitily.SYNC_CREATE)
                : capabitilies.contains(
                ConnectorInstance.Capabitily.ASYNC_CREATE)) {

            result = connector.create(oclass, attrs, options);
            if (result == null) {
                throw new IllegalStateException("Error creating user");
            }
        }

        return result;
    }

    public Uid resolveUsername(final ObjectClass objectClass,
            final String username,
            final OperationOptions options) {

        Uid result = null;

        if (capabitilies.contains(ConnectorInstance.Capabitily.RESOLVE)) {
            result = connector.resolveUsername(objectClass, username, options);
        }

        return result;
    }

    public Uid update(final PropagationManager.PropagationMode propagationMode,
            final ObjectClass objclass,
            final Uid uid,
            final Set<Attribute> replaceAttributes,
            final OperationOptions options) {

        Uid result = uid;

        if (propagationMode == PropagationManager.PropagationMode.SYNC
                ? capabitilies.contains(
                ConnectorInstance.Capabitily.SYNC_UPDATE)
                : capabitilies.contains(
                ConnectorInstance.Capabitily.ASYNC_UPDATE)) {

            result = connector.update(
                    objclass, uid, replaceAttributes, options);
            if (result == null) {
                throw new IllegalStateException("Error updating user");
            }
        }

        return result;
    }

    public void delete(final PropagationManager.PropagationMode propagationMode,
            final ObjectClass objClass,
            final Uid uid,
            final OperationOptions options) {

        if (propagationMode == PropagationManager.PropagationMode.SYNC
                ? capabitilies.contains(
                ConnectorInstance.Capabitily.SYNC_DELETE)
                : capabitilies.contains(
                ConnectorInstance.Capabitily.ASYNC_DELETE)) {

            connector.delete(objClass, uid, options);
        }
    }

    public void validate() {
        connector.validate();
    }

    @Override
    public String toString() {
        return "ConnectorFacadeProxy{"
                + "connector=" + connector
                + "capabitilies=" + capabitilies + '}';
    }
}
