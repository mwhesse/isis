/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.alternatives.remoting.common.facade.impl;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;

import org.apache.isis.alternatives.remoting.common.IsisRemoteException;
import org.apache.isis.alternatives.remoting.common.client.transaction.ClientTransactionEvent;
import org.apache.isis.alternatives.remoting.common.data.Data;
import org.apache.isis.alternatives.remoting.common.data.common.EncodableObjectData;
import org.apache.isis.alternatives.remoting.common.data.common.IdentityData;
import org.apache.isis.alternatives.remoting.common.data.common.NullData;
import org.apache.isis.alternatives.remoting.common.data.common.ObjectData;
import org.apache.isis.alternatives.remoting.common.data.common.ReferenceData;
import org.apache.isis.alternatives.remoting.common.data.query.PersistenceQueryData;
import org.apache.isis.alternatives.remoting.common.exchange.AuthorizationRequestUsability;
import org.apache.isis.alternatives.remoting.common.exchange.AuthorizationRequestVisibility;
import org.apache.isis.alternatives.remoting.common.exchange.AuthorizationResponse;
import org.apache.isis.alternatives.remoting.common.exchange.ClearAssociationRequest;
import org.apache.isis.alternatives.remoting.common.exchange.ClearAssociationResponse;
import org.apache.isis.alternatives.remoting.common.exchange.ClearValueRequest;
import org.apache.isis.alternatives.remoting.common.exchange.ClearValueResponse;
import org.apache.isis.alternatives.remoting.common.exchange.CloseSessionRequest;
import org.apache.isis.alternatives.remoting.common.exchange.CloseSessionResponse;
import org.apache.isis.alternatives.remoting.common.exchange.ExecuteClientActionRequest;
import org.apache.isis.alternatives.remoting.common.exchange.ExecuteClientActionResponse;
import org.apache.isis.alternatives.remoting.common.exchange.ExecuteServerActionRequest;
import org.apache.isis.alternatives.remoting.common.exchange.ExecuteServerActionResponse;
import org.apache.isis.alternatives.remoting.common.exchange.FindInstancesRequest;
import org.apache.isis.alternatives.remoting.common.exchange.FindInstancesResponse;
import org.apache.isis.alternatives.remoting.common.exchange.GetObjectRequest;
import org.apache.isis.alternatives.remoting.common.exchange.GetObjectResponse;
import org.apache.isis.alternatives.remoting.common.exchange.GetPropertiesRequest;
import org.apache.isis.alternatives.remoting.common.exchange.GetPropertiesResponse;
import org.apache.isis.alternatives.remoting.common.exchange.HasInstancesRequest;
import org.apache.isis.alternatives.remoting.common.exchange.HasInstancesResponse;
import org.apache.isis.alternatives.remoting.common.exchange.KnownObjectsRequest;
import org.apache.isis.alternatives.remoting.common.exchange.OidForServiceRequest;
import org.apache.isis.alternatives.remoting.common.exchange.OidForServiceResponse;
import org.apache.isis.alternatives.remoting.common.exchange.OpenSessionRequest;
import org.apache.isis.alternatives.remoting.common.exchange.OpenSessionResponse;
import org.apache.isis.alternatives.remoting.common.exchange.ResolveFieldRequest;
import org.apache.isis.alternatives.remoting.common.exchange.ResolveFieldResponse;
import org.apache.isis.alternatives.remoting.common.exchange.ResolveObjectRequest;
import org.apache.isis.alternatives.remoting.common.exchange.ResolveObjectResponse;
import org.apache.isis.alternatives.remoting.common.exchange.SetAssociationRequest;
import org.apache.isis.alternatives.remoting.common.exchange.SetAssociationResponse;
import org.apache.isis.alternatives.remoting.common.exchange.SetValueRequest;
import org.apache.isis.alternatives.remoting.common.exchange.SetValueResponse;
import org.apache.isis.alternatives.remoting.common.facade.ServerFacade;
import org.apache.isis.alternatives.remoting.common.protocol.ObjectEncoderDecoder;
import org.apache.isis.applib.Identifier;
import org.apache.isis.core.commons.authentication.AuthenticationSession;
import org.apache.isis.core.commons.config.ConfigurationConstants;
import org.apache.isis.core.commons.config.IsisConfiguration;
import org.apache.isis.core.commons.ensure.Assert;
import org.apache.isis.core.commons.exceptions.IsisException;
import org.apache.isis.core.commons.exceptions.UnexpectedCallException;
import org.apache.isis.core.commons.exceptions.UnknownTypeException;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.ResolveState;
import org.apache.isis.core.metamodel.adapter.oid.Oid;
import org.apache.isis.core.metamodel.adapter.version.Version;
import org.apache.isis.core.metamodel.facetedmethod.IdentifierUtils;
import org.apache.isis.core.metamodel.facets.collections.modify.CollectionFacet;
import org.apache.isis.core.metamodel.facets.collections.modify.CollectionFacetUtils;
import org.apache.isis.core.metamodel.facets.object.encodeable.EncodableFacet;
import org.apache.isis.core.metamodel.spec.ActionType;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.SpecificationLoader;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.core.metamodel.spec.feature.ObjectMember;
import org.apache.isis.core.metamodel.spec.feature.OneToManyAssociation;
import org.apache.isis.core.metamodel.spec.feature.OneToOneAssociation;
import org.apache.isis.core.metamodel.specloader.specimpl.ObjectActionImpl;
import org.apache.isis.core.runtime.authentication.AuthenticationManager;
import org.apache.isis.core.runtime.authentication.AuthenticationRequestPassword;
import org.apache.isis.core.runtime.context.IsisContext;
import org.apache.isis.core.runtime.persistence.PersistenceConstants;
import org.apache.isis.core.runtime.persistence.PersistenceSession;
import org.apache.isis.core.runtime.persistence.query.PersistenceQuery;
import org.apache.isis.core.runtime.transaction.IsisTransactionManager;
import org.apache.isis.core.runtime.transaction.messagebroker.MessageBroker;
import org.apache.isis.core.runtime.transaction.updatenotifier.UpdateNotifier;

/**
 * previously called <tt>ServerDistribution</tt>.
 */
public class ServerFacadeImpl implements ServerFacade {

    private static final Logger LOG = Logger.getLogger(ServerFacadeImpl.class);

    private final AuthenticationManager authenticationManager;
    private ObjectEncoderDecoder encoderDecoder;

    public ServerFacadeImpl(final AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    // //////////////////////////////////////////////////////////////
    // init, shutdown
    // //////////////////////////////////////////////////////////////

    @Override
    public void init() {
    }

    @Override
    public void shutdown() {
    }

    // //////////////////////////////////////////////////////////////
    // Authentication
    // //////////////////////////////////////////////////////////////

    @Override
    public OpenSessionResponse openSession(OpenSessionRequest request2) {
        final AuthenticationRequestPassword request =
            new AuthenticationRequestPassword(request2.getUsername(), request2.getPassword());
        final AuthenticationSession session = authenticationManager.authenticate(request);
        return new OpenSessionResponse(session);
    }

    @Override
    public CloseSessionResponse closeSession(CloseSessionRequest request) {
        authenticationManager.closeSession(request.getSession());
        return new CloseSessionResponse();
    }

    // //////////////////////////////////////////////////////////////
    // Authorization
    // //////////////////////////////////////////////////////////////

    @Override
    public AuthorizationResponse authorizeVisibility(AuthorizationRequestVisibility request) {
        ObjectAdapter targetAdapter = encoderDecoder.decode(request.getTarget());
        boolean allowed = getMember(request.getIdentifier()).isVisible(request.getSession(), targetAdapter).isAllowed();
        return encoderDecoder.encodeAuthorizeResponse(allowed);
    }

    @Override
    public AuthorizationResponse authorizeUsability(AuthorizationRequestUsability request) {
        ObjectAdapter targetAdapter = encoderDecoder.decode(request.getTarget());
        boolean allowed = getMember(request.getIdentifier()).isUsable(request.getSession(), targetAdapter).isAllowed();
        return encoderDecoder.encodeAuthorizeResponse(allowed);
    }

    private ObjectMember getMember(final String memberName) {
        final Identifier id = IdentifierUtils.fromIdentityString(memberName);
        final ObjectSpecification specification = getSpecificationLoader().loadSpecification(id.getClassName());
        if (id.isPropertyOrCollection()) {
            return getAssociationElseThrowException(id, specification);
        } else {
            return getActionElseThrowException(id, specification);
        }
    }

    private ObjectMember getActionElseThrowException(final Identifier id, final ObjectSpecification specification) {
        ObjectMember member =
            specification.getObjectAction(ActionType.USER, id.getMemberName(),
                loadParameterSpecifications(id));
        if (member == null) {
            throw new IsisException("No user action found for id " + id);
        }
        return member;
    }

    private ObjectMember getAssociationElseThrowException(final Identifier id, final ObjectSpecification specification) {
        ObjectMember member = specification.getAssociation(id.getMemberName());
        if (member == null) {
            throw new IsisException("No property or collection found for id " + id);
        }
        return member;
    }

    private static List<ObjectSpecification> loadParameterSpecifications(final Identifier id) {
        final List<String> parameters = id.getMemberParameterNames();
        final List<ObjectSpecification> specifications = Lists.newArrayList();
        for (String parameter : parameters) {
            specifications.add(getSpecificationLoader().loadSpecification(parameter));
        }
        return specifications;
    }

    // //////////////////////////////////////////////////////////////
    // Misc
    // //////////////////////////////////////////////////////////////

    @Override
    public GetPropertiesResponse getProperties(GetPropertiesRequest request) {
        final Properties properties = new Properties();
        properties.put("test-client", "true");

        // pass over services
        final IsisConfiguration configuration = IsisContext.getConfiguration();
        final IsisConfiguration serviceProperties =
            configuration.getProperties(ConfigurationConstants.ROOT + "services");
        
        for(String propertyName: serviceProperties) {
            properties.put(propertyName, serviceProperties.getString(propertyName));
        }

        // pass over OID generator
        final String oidGeneratorClass = getPersistenceSession().getOidGenerator().getClass().getName();
        if (oidGeneratorClass != null) {
            properties.put(PersistenceConstants.OID_GENERATOR_CLASS_NAME, oidGeneratorClass);
        }

        // TODO load up client-side properties
        return new GetPropertiesResponse(properties);
    }

    // //////////////////////////////////////////////////////////////
    // setAssociation, setValue, clearAssociation, clearValue
    // //////////////////////////////////////////////////////////////

    /**
     * Applies to both {@link OneToOneAssociation}s and {@link OneToManyAssociation}s.
     */
    @Override
    public SetAssociationResponse setAssociation(final SetAssociationRequest request) {

        AuthenticationSession session = request.getSession();
        String fieldIdentifier = request.getFieldIdentifier();
        IdentityData targetData = request.getTarget();
        IdentityData associateData = request.getAssociate();

        if (LOG.isDebugEnabled()) {
            LOG.debug("request setAssociation " + fieldIdentifier + " on " + targetData + " with " + associateData
                + " for " + session);
        }

        final ObjectAdapter targetAdapter = getPersistentObjectAdapter(session, targetData);
        final ObjectAdapter associate = getPersistentObjectAdapter(session, associateData);
        final ObjectAssociation association = targetAdapter.getSpecification().getAssociation(fieldIdentifier);

        ensureAssociationModifiableElseThrowException(session, targetAdapter, association);

        if (association instanceof OneToOneAssociation) {
            ((OneToOneAssociation) association).setAssociation(targetAdapter, associate);
        } else {
            ((OneToManyAssociation) association).addElement(targetAdapter, associate);
        }

        return new SetAssociationResponse(getUpdates());
    }

    /**
     * Applies only for {@link OneToOneAssociation}s.
     */
    @Override
    public SetValueResponse setValue(SetValueRequest request) {

        AuthenticationSession session = request.getSession();
        String fieldIdentifier = request.getFieldIdentifier();
        IdentityData targetIdentityData = request.getTarget();
        EncodableObjectData encodeableObjectData = request.getValue();

        Assert.assertNotNull(encodeableObjectData);
        if (LOG.isDebugEnabled()) {
            LOG.debug("request setValue " + fieldIdentifier + " on " + targetIdentityData + " with "
                + encodeableObjectData + " for " + session);
        }

        final ObjectAdapter targetAdapter = getPersistentObjectAdapter(session, targetIdentityData);
        final OneToOneAssociation association =
            (OneToOneAssociation) targetAdapter.getSpecification().getAssociation(fieldIdentifier);

        ensureAssociationModifiableElseThrowException(session, targetAdapter, association);

        final String encodedObject = encodeableObjectData.getEncodedObjectData();

        final ObjectSpecification specification = association.getSpecification();
        final ObjectAdapter adapter = restoreLeafObject(encodedObject, specification);
        association.setAssociation(targetAdapter, adapter);

        return new SetValueResponse(getUpdates());
    }

    /**
     * Applies to both {@link OneToOneAssociation}s and {@link OneToManyAssociation}s.
     */
    @Override
    public ClearAssociationResponse clearAssociation(final ClearAssociationRequest request) {

        AuthenticationSession session = request.getSession();
        String fieldIdentifier = request.getFieldIdentifier();
        IdentityData targetData = request.getTarget();
        IdentityData associateData = request.getAssociate();

        if (LOG.isDebugEnabled()) {
            LOG.debug("request clearAssociation " + fieldIdentifier + " on " + targetData + " of " + associateData
                + " for " + session);
        }

        final ObjectAdapter targetAdapter = getPersistentObjectAdapter(session, targetData);
        final ObjectAdapter associateAdapter = getPersistentObjectAdapter(session, associateData);
        final ObjectSpecification specification = targetAdapter.getSpecification();
        final ObjectAssociation association = specification.getAssociation(fieldIdentifier);

        if (!association.isVisible(session, targetAdapter).isAllowed()
            || association.isUsable(session, targetAdapter).isVetoed()) {
            throw new IsisException("can't modify field as not visible or editable");
        }
        ensureAssociationModifiableElseThrowException(session, targetAdapter, association);

        if (association instanceof OneToOneAssociation) {
            ((OneToOneAssociation) association).clearAssociation(targetAdapter);
        } else {
            ((OneToManyAssociation) association).removeElement(targetAdapter, associateAdapter);
        }
        return new ClearAssociationResponse(getUpdates());
    }

    /**
     * Applies only for {@link OneToOneAssociation}s.
     */
    @Override
    public ClearValueResponse clearValue(final ClearValueRequest request) {

        AuthenticationSession session = request.getSession();
        String fieldIdentifier = request.getFieldIdentifier();
        IdentityData targetIdentityData = request.getTarget();

        if (LOG.isDebugEnabled()) {
            LOG.debug("request clearValue " + fieldIdentifier + " on " + targetIdentityData + " for " + session);
        }

        final ObjectAdapter targetAdapter = getPersistentObjectAdapter(session, targetIdentityData);
        final OneToOneAssociation association =
            (OneToOneAssociation) targetAdapter.getSpecification().getAssociation(fieldIdentifier);

        ensureAssociationModifiableElseThrowException(session, targetAdapter, association);

        association.clearAssociation(targetAdapter);
        return new ClearValueResponse(getUpdates());
    }

    private void ensureAssociationModifiableElseThrowException(final AuthenticationSession session,
        final ObjectAdapter targetAdapter, final ObjectAssociation association) {
        if (!association.isVisible(session, targetAdapter).isAllowed()
            || association.isUsable(session, targetAdapter).isVetoed()) {
            throw new IsisException("can't modify field as not visible or editable");
        }
    }

    // //////////////////////////////////////////////////////////////
    // executeClientAction
    // //////////////////////////////////////////////////////////////

    @Override
    public ExecuteClientActionResponse executeClientAction(ExecuteClientActionRequest request) {

        AuthenticationSession session = request.getSession();
        ReferenceData[] data = request.getData();
        int[] types = request.getTypes();

        if (LOG.isDebugEnabled()) {
            LOG.debug("execute client action for " + session);
            LOG.debug("start transaction");
        }

        // although the PersistenceSession will also do xactn mgmt, because we
        // are potentially modifying several objects we should explicitly define
        // the xactn at this level.
        getTransactionManager().startTransaction();
        try {
            final KnownObjectsRequest knownObjects = new KnownObjectsRequest();
            final ObjectAdapter[] persistedObjects = new ObjectAdapter[data.length];
            final ObjectAdapter[] disposedObjects = new ObjectAdapter[data.length];
            final ObjectAdapter[] changedObjects = new ObjectAdapter[data.length];
            for (int i = 0; i < data.length; i++) {
                ObjectAdapter object;
                switch (types[i]) {
                    case ClientTransactionEvent.ADD:
                        object = encoderDecoder.decode(data[i], knownObjects);
                        persistedObjects[i] = object;
                        if (object.getOid().isTransient()) { // confirm that the graph has not been made
                            // persistent earlier in this loop
                            LOG.debug("  makePersistent " + data[i]);
                            getPersistenceSession().makePersistent(object);
                        }
                        break;

                    case ClientTransactionEvent.CHANGE:
                        final ObjectAdapter obj = getPersistentObjectAdapter(data[i]);
                        obj.checkLock(data[i].getVersion());

                        object = encoderDecoder.decode(data[i], knownObjects);
                        LOG.debug("  objectChanged " + data[i]);
                        getPersistenceSession().objectChanged(object);
                        changedObjects[i] = object;
                        break;

                    case ClientTransactionEvent.DELETE:
                        final ObjectAdapter inObject = getPersistentObjectAdapter(data[i]);
                        inObject.checkLock(data[i].getVersion());

                        LOG.debug("  destroyObject " + data[i] + " for " + session);
                        disposedObjects[i] = inObject;
                        getPersistenceSession().destroyObject(inObject);
                        break;
                }

            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("  end transaction");
            }
            getTransactionManager().endTransaction();

            final ReferenceData[] madePersistent = new ReferenceData[data.length];
            final Version[] changedVersion = new Version[data.length];

            for (int i = 0; i < data.length; i++) {
                switch (types[i]) {
                    case ClientTransactionEvent.ADD:
                        madePersistent[i] = encoderDecoder.encodeIdentityData(persistedObjects[i]);
                        break;

                    case ClientTransactionEvent.CHANGE:
                        changedVersion[i] = changedObjects[i].getVersion();
                        break;

                }
            }

            return encoderDecoder.encodeClientActionResult(madePersistent, changedVersion, getUpdates());
        } catch (final RuntimeException e) {
            LOG.info("abort transaction", e);
            getTransactionManager().abortTransaction();
            throw e;
        }
    }

    // //////////////////////////////////////////////////////////////
    // executeServerAction
    // //////////////////////////////////////////////////////////////

    @Override
    public ExecuteServerActionResponse executeServerAction(ExecuteServerActionRequest request) {

        AuthenticationSession session = request.getSession();
        ActionType actionType = request.getActionType();
        String actionIdentifier = request.getActionIdentifier();
        ReferenceData targetData = request.getTarget();
        Data[] parameterData = request.getParameters();

        if (LOG.isDebugEnabled()) {
            LOG.debug("request executeAction " + actionIdentifier + " on " + targetData + " for " + session);
        }

        final KnownObjectsRequest knownObjects = new KnownObjectsRequest();

        ObjectAdapter targetAdapter = decodeTargetAdapter(session, targetData, knownObjects);

        final ObjectAction action = targetAdapter.getSpecification().getObjectAction(actionType, actionIdentifier);
        final ObjectAdapter[] parameters = decodeParameters(session, parameterData, knownObjects);

        if (action == null) {
            throw new IsisRemoteException("Could not find method " + actionIdentifier);
        }

        final ObjectAdapter resultAdapter = action.execute(targetAdapter, parameters);

        ObjectData persistedTargetData;
        if (targetData == null) {
            persistedTargetData = null;
        } else if (targetData instanceof ObjectData) {
            persistedTargetData = encoderDecoder.encodeMadePersistentGraph((ObjectData) targetData, targetAdapter);
        } else {
            persistedTargetData = null;
        }

        final ObjectData[] persistedParameterData = new ObjectData[parameterData.length];
        for (int i = 0; i < persistedParameterData.length; i++) {
            if (action.getParameters().get(i).getSpecification().isNotCollection()
                && parameterData[i] instanceof ObjectData) {
                persistedParameterData[i] =
                    encoderDecoder.encodeMadePersistentGraph((ObjectData) parameterData[i], parameters[i]);
            }
        }
        final List<String> messages = getMessageBroker().getMessages();
        final List<String> warnings = getMessageBroker().getWarnings();

        // TODO for efficiency, need to remove the objects in the results graph from the updates set
        return encoderDecoder.encodeServerActionResult(resultAdapter, getUpdates(), getDisposed(), persistedTargetData,
            persistedParameterData, messages.toArray(new String[0]), warnings.toArray(new String[0]));
    }

    private ObjectAdapter decodeTargetAdapter(AuthenticationSession session, ReferenceData targetData,
        final KnownObjectsRequest knownObjects) {
        ObjectAdapter targetAdapter;
        if (targetData == null) {
            throw new IsisRemoteException("No target specified");
        } else if (targetData instanceof IdentityData) {
            targetAdapter = getPersistentObjectAdapter(session, (IdentityData) targetData);
        } else if (targetData instanceof ObjectData) {
            targetAdapter = encoderDecoder.decode(targetData, knownObjects);
        } else {
            // not expected
            throw new IsisException();
        }
        return targetAdapter;
    }

    @SuppressWarnings("unused")
    private ObjectAction getActionMethod(final String actionType, final String actionIdentifier,
        final Data[] parameterData, final ObjectAdapter adapter) {
        final List<ObjectSpecification> parameterSpecs = Lists.newArrayList();
        for (int i = 0; i < parameterSpecs.size(); i++) {
            parameterSpecs.add(getSpecification(parameterData[i].getType()));
        }

        final ActionType type = ObjectActionImpl.getType(actionType);

        final int pos = actionIdentifier.indexOf('#');

        final String methodName = actionIdentifier.substring(pos + 1);

        if (adapter == null) {
            throw new UnexpectedCallException("object not specified");
        }
        return adapter.getSpecification().getObjectAction(type, methodName, parameterSpecs);
    }

    private ObjectAdapter[] decodeParameters(final AuthenticationSession session, final Data[] parameterData,
        final KnownObjectsRequest knownObjects) {
        final ObjectAdapter[] parameters = new ObjectAdapter[parameterData.length];
        for (int i = 0; i < parameters.length; i++) {
            final Data data = parameterData[i];
            if (data instanceof NullData) {
                continue;
            }

            if (data instanceof IdentityData) {
                parameters[i] = getPersistentObjectAdapter(session, (IdentityData) data);
            } else if (data instanceof ObjectData) {
                parameters[i] = encoderDecoder.decode(data, knownObjects);
            } else if (data instanceof EncodableObjectData) {
                final ObjectSpecification valueSpecification =
                    getSpecificationLoader().loadSpecification(data.getType());
                final String valueData = ((EncodableObjectData) data).getEncodedObjectData();

                final ObjectAdapter value = restoreLeafObject(valueData, valueSpecification);
                /*
                 * ObjectAdapter value = IsisContext.getObjectLoader().createValueInstance(valueSpecification);
                 * value.restoreFromEncodedString(valueData);
                 */
                parameters[i] = value;
            } else {
                throw new UnknownTypeException(data);
            }
        }
        return parameters;
    }

    private ReferenceData[] getDisposed() {
        final List<ReferenceData> list = new ArrayList<ReferenceData>();
        for (ObjectAdapter element : getUpdateNotifier().getDisposedObjects()) {
            list.add(encoderDecoder.encodeIdentityData(element));
        }
        return list.toArray(new ReferenceData[list.size()]);
    }

    // //////////////////////////////////////////////////////////////
    // getObject, resolve
    // //////////////////////////////////////////////////////////////

    @Override
    public GetObjectResponse getObject(GetObjectRequest request) {

        Oid oid = request.getOid();
        String specificationName = request.getSpecificationName();

        final ObjectSpecification specification = getSpecification(specificationName);
        final ObjectAdapter adapter = getPersistenceSession().loadObject(oid, specification);

        return new GetObjectResponse(encoderDecoder.encodeForUpdate(adapter));
    }

    @Override
    public ResolveFieldResponse resolveField(ResolveFieldRequest request) {

        AuthenticationSession session = request.getSession();
        IdentityData targetData = request.getTarget();
        String fieldIdentifier = request.getFieldIdentifier();

        if (LOG.isDebugEnabled()) {
            LOG.debug("request resolveField " + targetData + "/" + fieldIdentifier + " for " + session);
        }

        final ObjectSpecification spec = getSpecification(targetData.getType());
        final ObjectAssociation field = spec.getAssociation(fieldIdentifier);
        final ObjectAdapter targetAdapter = getPersistenceSession().recreateAdapter(targetData.getOid(), spec);

        getPersistenceSession().resolveField(targetAdapter, field);
        Data data = encoderDecoder.encodeForResolveField(targetAdapter, fieldIdentifier);
        return new ResolveFieldResponse(data);
    }

    @Override
    public ResolveObjectResponse resolveImmediately(ResolveObjectRequest request) {

        AuthenticationSession session = request.getSession();
        IdentityData targetData = request.getTarget();

        if (LOG.isDebugEnabled()) {
            LOG.debug("request resolveImmediately " + targetData + " for " + session);
        }

        final ObjectSpecification spec = getSpecification(targetData.getType());
        final ObjectAdapter object = getPersistenceSession().loadObject(targetData.getOid(), spec);

        if (object.getResolveState().canChangeTo(ResolveState.RESOLVING)) {
            // this is need when the object store does not load the object fully in the getObject() above
            getPersistenceSession().resolveImmediately(object);
        }

        return new ResolveObjectResponse(encoderDecoder.encodeCompletePersistentGraph(object));
    }

    // //////////////////////////////////////////////////////////////
    // findInstances, hasInstances
    // //////////////////////////////////////////////////////////////

    @Override
    public FindInstancesResponse findInstances(final FindInstancesRequest request) {

        AuthenticationSession session = request.getSession();
        PersistenceQueryData criteriaData = request.getCriteria();

        final PersistenceQuery criteria = encoderDecoder.decodePersistenceQuery(criteriaData);
        LOG.debug("request findInstances " + criteria + " for " + session);
        final ObjectAdapter instances = getPersistenceSession().findInstances(criteria);
        ObjectData[] instancesData = convertToCollectionAdapter(instances);
        return new FindInstancesResponse(instancesData);
    }

    @Override
    public HasInstancesResponse hasInstances(HasInstancesRequest request) {

        AuthenticationSession session = request.getSession();
        String specificationName = request.getSpecificationName();

        if (LOG.isDebugEnabled()) {
            LOG.debug("request hasInstances of " + specificationName + " for " + session);
        }
        boolean hasInstances = getPersistenceSession().hasInstances(getSpecification(specificationName));
        return new HasInstancesResponse(hasInstances);
    }

    private ObjectData[] convertToCollectionAdapter(final ObjectAdapter instances) {
        final CollectionFacet facet = CollectionFacetUtils.getCollectionFacetFromSpec(instances);
        final ObjectData[] data = new ObjectData[facet.size(instances)];
        final Enumeration elements = facet.elements(instances);
        int i = 0;
        while (elements.hasMoreElements()) {
            final ObjectAdapter element = (ObjectAdapter) elements.nextElement();
            data[i++] = encoderDecoder.encodeCompletePersistentGraph(element);
        }
        return data;
    }

    // //////////////////////////////////////////////////////////////
    // oidForService
    // //////////////////////////////////////////////////////////////

    @Override
    public OidForServiceResponse oidForService(OidForServiceRequest request) {

        String serviceId = request.getServiceId();

        final ObjectAdapter serviceAdapter = getPersistenceSession().getService(serviceId);
        if (serviceAdapter == null) {
            throw new IsisRemoteException("Failed to find service " + serviceId);
        }
        return new OidForServiceResponse(encoderDecoder.encodeIdentityData(serviceAdapter));
    }

    // //////////////////////////////////////////////////////////////
    // Helpers
    // //////////////////////////////////////////////////////////////

    private ObjectSpecification getSpecification(final String fullName) {
        return getSpecificationLoader().loadSpecification(fullName);
    }

    private ObjectAdapter getPersistentObjectAdapter(final AuthenticationSession session, final IdentityData object) {
        final ObjectAdapter obj = getPersistentObjectAdapter(object);
        if (LOG.isDebugEnabled()) {
            LOG.debug("get object " + object + " for " + session + " --> " + obj);
        }
        obj.checkLock(object.getVersion());
        return obj;
    }

    private ObjectAdapter getPersistentObjectAdapter(final ReferenceData object) {
        final ObjectSpecification spec = getSpecification(object.getType());
        final ObjectAdapter obj = getPersistenceSession().loadObject(object.getOid(), spec);
        Assert.assertNotNull(obj);
        return obj;
    }

    private ObjectAdapter restoreLeafObject(final String encodedObject, final ObjectSpecification specification) {
        final EncodableFacet encoder = specification.getFacet(EncodableFacet.class);
        if (encoder == null) {
            throw new IsisException("No encoder for " + specification.getFullIdentifier());
        }
        final ObjectAdapter object = encoder.fromEncodedString(encodedObject);
        return object;
    }

    private ObjectData[] getUpdates() {
        final List<ObjectData> list = new ArrayList<ObjectData>();
        for (ObjectAdapter element : getUpdateNotifier().getChangedObjects()) {
            list.add(encoderDecoder.encodeForUpdate(element));
        }
        return list.toArray(new ObjectData[list.size()]);
    }

    // //////////////////////////////////////////////////////////////
    // Dependencies (injected)
    // //////////////////////////////////////////////////////////////

    public void setEncoder(final ObjectEncoderDecoder objectEncoder) {
        this.encoderDecoder = objectEncoder;
    }

    // //////////////////////////////////////////////////////////////
    // Dependencies (from context)
    // //////////////////////////////////////////////////////////////

    private static SpecificationLoader getSpecificationLoader() {
        return IsisContext.getSpecificationLoader();
    }

    private static PersistenceSession getPersistenceSession() {
        return IsisContext.getPersistenceSession();
    }

    private static IsisTransactionManager getTransactionManager() {
        return getPersistenceSession().getTransactionManager();
    }

    private static UpdateNotifier getUpdateNotifier() {
        return IsisContext.getUpdateNotifier();
    }

    private static MessageBroker getMessageBroker() {
        return IsisContext.getMessageBroker();
    }

}
