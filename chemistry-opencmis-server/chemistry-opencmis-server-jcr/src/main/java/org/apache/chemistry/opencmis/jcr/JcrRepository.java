/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.chemistry.opencmis.jcr;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventJournal;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ChangeEventInfo;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.CapabilityAcl;
import org.apache.chemistry.opencmis.commons.enums.CapabilityChanges;
import org.apache.chemistry.opencmis.commons.enums.CapabilityContentStreamUpdates;
import org.apache.chemistry.opencmis.commons.enums.CapabilityJoin;
import org.apache.chemistry.opencmis.commons.enums.CapabilityQuery;
import org.apache.chemistry.opencmis.commons.enums.CapabilityRenditions;
import org.apache.chemistry.opencmis.commons.enums.ChangeType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisPermissionDeniedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisUpdateConflictException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ChangeEventInfoDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderContainerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectParentDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryCapabilitiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryInfoImpl;
import org.apache.chemistry.opencmis.commons.server.ObjectInfoHandler;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.apache.chemistry.opencmis.jcr.query.QueryTranslator;
import org.apache.chemistry.opencmis.jcr.type.JcrDocumentTypeHandler;
import org.apache.chemistry.opencmis.jcr.type.JcrFolderTypeHandler;
import org.apache.chemistry.opencmis.jcr.type.JcrTypeHandlerManager;
import org.apache.chemistry.opencmis.jcr.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JCR back-end for CMIS server.
 */
public class JcrRepository {
    private static final Logger log = LoggerFactory.getLogger(JcrRepository.class);

    protected final Repository repository;
    protected final JcrTypeManager typeManager;
    protected final PathManager pathManager;
    protected final JcrTypeHandlerManager typeHandlerManager;

    /**
     * Create a new <code>JcrRepository</code> instance backed by a JCR repository.
     *
     * @param repository  the JCR repository
     * @param pathManager
     * @param typeManager  
     * @param typeHandlerManager
     */
    public JcrRepository(Repository repository, PathManager pathManager, JcrTypeManager typeManager, JcrTypeHandlerManager typeHandlerManager) {
        this.repository = repository;
        this.typeManager = typeManager;
        this.typeHandlerManager = typeHandlerManager;
        this.pathManager = pathManager;
    }

    /**
     * Logger into the underlying JCR repository.
     * 
     * @param credentials
     * @param workspaceName
     * @return
     * @throws LoginException
     * @throws NoSuchWorkspaceException
     * @throws RepositoryException
     */
    public Session login(Credentials credentials, String workspaceName) {
        try {
            return repository.login(credentials, workspaceName);
        }
        catch (LoginException e) {
            log.debug(e.getMessage(), e);
            throw new CmisPermissionDeniedException(e.getMessage(), e);
        }
        catch (NoSuchWorkspaceException e) {
            log.debug(e.getMessage(), e);
            throw new CmisObjectNotFoundException(e.getMessage(), e);
        }
        catch (RepositoryException e) {
            log.debug(e.getMessage(), e);
            throw new CmisRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * See CMIS 1.0 section 2.2.2.2 getRepositoryInfo
     */
    public RepositoryInfo getRepositoryInfo(Session session) {
        log.debug("getRepositoryInfo");

        return compileRepositoryInfo(session.getWorkspace().getName());
    }

    /**
     * See CMIS 1.0 section 2.2.2.2 getRepositoryInfo
     */
    public List<RepositoryInfo> getRepositoryInfos(Session session) {
        try {
            ArrayList<RepositoryInfo> infos = new ArrayList<RepositoryInfo>();
            for (String wspName : session.getWorkspace().getAccessibleWorkspaceNames()) {
                infos.add(compileRepositoryInfo(wspName));
            }

            return infos;
        }
        catch (RepositoryException e) {
            log.debug(e.getMessage(), e);
            throw new CmisRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * See CMIS 1.0 section 2.2.2.3 getTypeChildren
     */
    public TypeDefinitionList getTypeChildren(Session session, String typeId, boolean includePropertyDefinitions,
            BigInteger maxItems, BigInteger skipCount) {
        
        log.debug("getTypesChildren");
        return typeManager.getTypeChildren(typeId, includePropertyDefinitions, maxItems, skipCount);
    }

    /**
     * See CMIS 1.0 section 2.2.2.5 getTypeDefinition
     */
    public TypeDefinition getTypeDefinition(Session session, String typeId) {
        log.debug("getTypeDefinition");

        TypeDefinition type = typeManager.getType(typeId);
        if (type == null) {
            throw new CmisObjectNotFoundException("Type '" + typeId + "' is unknown!");
        }

        return JcrTypeManager.copyTypeDefinition(type);
    }

    /**
     * See CMIS 1.0 section 2.2.2.4 getTypeDescendants
     */
    public List<TypeDefinitionContainer> getTypesDescendants(Session session, String typeId, BigInteger depth,
            Boolean includePropertyDefinitions) {

        log.debug("getTypesDescendants");
        return typeManager.getTypesDescendants(typeId, depth, includePropertyDefinitions);
    }

    /**
     * See CMIS 1.0 section 2.2.4.1 createDocument
     */
    public String createDocument(Session session, Properties properties, String folderId, ContentStream contentStream,
            VersioningState versioningState) {

        log.debug("createDocument");

        // check properties
        if (properties == null || properties.getProperties() == null) {
            throw new CmisInvalidArgumentException("Properties must be set!");
        }

        // check type
        String typeId = PropertyHelper.getTypeId(properties);
        TypeDefinition type = typeManager.getType(typeId);
        if (type == null) {
            throw new CmisObjectNotFoundException("Type '" + typeId + "' is unknown!");
        }

        boolean isVersionable = JcrTypeManager.isVersionable(type);
        if (!isVersionable && versioningState != VersioningState.NONE) {
            throw new CmisConstraintException("Versioning not supported for " + typeId);
        }

        if (isVersionable && versioningState == VersioningState.NONE) {
            throw new CmisConstraintException("Versioning required for " + typeId);
        }

        // get the name
        String name = PropertyHelper.getStringProperty(properties, PropertyIds.NAME);
        // get parent Node and create child
        JcrFolder parent = getJcrNode(session, folderId).asFolder();
        JcrDocumentTypeHandler typeHandler = typeHandlerManager.getDocumentTypeHandler(typeId);
        JcrNode jcrNode = typeHandler.createDocument(parent, JcrConverter.toJcrName(name), properties, contentStream, versioningState);
        return jcrNode.getId();
    }

    /**
     * See CMIS 1.0 section 2.2.4.2 createDocumentFromSource
     */
    public String createDocumentFromSource(Session session, String sourceId, Properties properties, String folderId,
            VersioningState versioningState) {

        log.debug("createDocumentFromSource");

        // get parent folder Node
        JcrFolder parent = getJcrNode(session, folderId).asFolder();

        // get source document Node
        JcrDocument source = getJcrNode(session, sourceId).asDocument();

        boolean isVersionable = source.isVersionable();
        if (!isVersionable && versioningState != VersioningState.NONE) {
            throw new CmisConstraintException("Versioning not supported for " + sourceId);
        }

        if (isVersionable && versioningState == VersioningState.NONE) {
            throw new CmisConstraintException("Versioning required for " + sourceId);
        }

        // create child from source
        JcrNode jcrNode = parent.addNodeFromSource(source, properties);
        return jcrNode.getId();
    }

    /**
     * See CMIS 1.0 section 2.2.4.3 createFolder
     */
    public String createFolder(Session session, Properties properties, String folderId) {
        log.debug("createFolder");

        // check properties
        if (properties == null || properties.getProperties() == null) {
            throw new CmisInvalidArgumentException("Properties must be set!");
        }

        // check type
        String typeId = PropertyHelper.getTypeId(properties);
        TypeDefinition type = typeManager.getType(typeId);
        if (type == null) {
            throw new CmisObjectNotFoundException("Type '" + typeId + "' is unknown!");
        }

        // get the name
        String name = PropertyHelper.getStringProperty(properties, PropertyIds.NAME);
        // get parent Node
        JcrFolder parent = getJcrNode(session, folderId).asFolder();
        JcrFolderTypeHandler typeHandler = typeHandlerManager.getFolderTypeHandler(typeId);
        JcrNode jcrNode = typeHandler.createFolder(parent, JcrConverter.toJcrName(name), properties);
        return jcrNode.getId();
    }

    /**
     * See CMIS 1.0 section 2.2.4.13 moveObject
     */
    public ObjectData moveObject(Session session, Holder<String> objectId, String targetFolderId,
            ObjectInfoHandler objectInfos, boolean requiresObjectInfo) {

        log.debug("moveObject");

        if (objectId == null || objectId.getValue() == null) {
            throw new CmisInvalidArgumentException("Id is not valid!");
        }

        // get the node and parent
        JcrNode jcrNode = getJcrNode(session, objectId.getValue());
        JcrFolder parent = getJcrNode(session, targetFolderId).asFolder();
        jcrNode = jcrNode.move(parent);
        objectId.setValue(jcrNode.getId());
        return jcrNode.compileObjectType(null, false, objectInfos, requiresObjectInfo);
    }

    /**
     * See CMIS 1.0 section 2.2.4.16 setContentStream
     */
    public void setContentStream(Session session, Holder<String> objectId, Boolean overwriteFlag,
            ContentStream contentStream) {

        log.debug("setContentStream or deleteContentStream");

        if (objectId == null || objectId.getValue() == null) {
            throw new CmisInvalidArgumentException("Id is not valid!");
        }

        JcrDocument jcrDocument = getJcrNode(session, objectId.getValue()).asDocument();
        String id = jcrDocument.setContentStream(contentStream, Boolean.TRUE.equals(overwriteFlag)).getId();
        objectId.setValue(id);
    }

    /**
     * See CMIS 1.0 section 2.2.4.14 deleteObject
     */
    public void deleteObject(Session session, String objectId, Boolean allVersions) {
        log.debug("deleteObject");

        // get the node
        JcrNode jcrNode = getJcrNode(session, objectId);
        try {
            // check on private copy
            boolean isPwc = jcrNode.isVersionable()
                    && JcrPrivateWorkingCopy.isPwc(jcrNode.asVersion().getVersionLabel());
            jcrNode.delete(Boolean.TRUE.equals(allVersions), isPwc);
        }
        catch(RepositoryException rex) {
            log.debug(rex.getMessage(), rex);
            throw new CmisRuntimeException(rex.getMessage(), rex);
        }
    }

    /**
     * See CMIS 1.0 section 2.2.4.15 deleteTree
     */
    public FailedToDeleteData deleteTree(Session session, String folderId) {
        log.debug("deleteTree");

        // get the folder
        JcrFolder jcrFolder = getJcrNode(session, folderId).asFolder();
        return jcrFolder.deleteTree();
    }

    /**
     * See CMIS 1.0 section 2.2.4.12 updateProperties
     */
    public ObjectData updateProperties(Session session, Holder<String> objectId, Properties properties,
            ObjectInfoHandler objectInfos, boolean objectInfoRequired) {

        log.debug("updateProperties");

        if (objectId == null) {
            throw new CmisInvalidArgumentException("Id is not valid!");
        }

        // get the node
        JcrNode jcrNode = getJcrNode(session, objectId.getValue());
        String id = jcrNode.updateProperties(properties).getId();
        objectId.setValue(id);
        return jcrNode.compileObjectType(null, false, objectInfos, objectInfoRequired);
    }

    /**
     * See CMIS 1.0 section 2.2.4.7 getObject
     */
    public ObjectData getObject(Session session, String objectId, String filter, Boolean includeAllowableActions,
            ObjectInfoHandler objectInfos, boolean requiresObjectInfo) {

        log.debug("getObject");

        // check id
        if (objectId == null) {
            throw new CmisInvalidArgumentException("Object Id must be set.");
        }

        // get the node
        JcrNode jcrNode = getJcrNode(session, objectId);

        // gather properties
        return jcrNode.compileObjectType(splitFilter(filter), includeAllowableActions, objectInfos, requiresObjectInfo);
    }

    /**
     * See CMIS 1.0 section 2.2.4.8 getProperties
     */
    public Properties getProperties(Session session, String objectId, String filter, Boolean includeAllowableActions,
            ObjectInfoHandler objectInfos, boolean requiresObjectInfo) {

        ObjectData object = getObject(session, objectId, filter, includeAllowableActions, objectInfos, requiresObjectInfo);
        return object.getProperties();
    }

    /**
     * See CMIS 1.0 section 2.2.4.6 getAllowableActions
     */
    public AllowableActions getAllowableActions(Session session, String objectId) {
        log.debug("getAllowableActions");

        JcrNode jcrNode = getJcrNode(session, objectId);
        return jcrNode.getAllowableActions();
    }

    /**
     * See CMIS 1.0 section 2.2.4.10 getContentStream
     */
    public ContentStream getContentStream(Session session, String objectId, BigInteger offset, BigInteger length) {
        log.debug("getContentStream");

        if (offset != null || length != null) {
            throw new CmisInvalidArgumentException("Offset and Length are not supported!");
        }

        // get the node
        JcrDocument jcrDocument = getJcrNode(session, objectId).asDocument();
        return jcrDocument.getContentStream();        
    }

    /**
     * See CMIS 1.0 section 2.2.3.1 getChildren
     */
    public ObjectInFolderList getChildren(Session session, String folderId, String filter,
            Boolean includeAllowableActions, Boolean includePathSegment, BigInteger maxItems, BigInteger skipCount,
            ObjectInfoHandler objectInfos, boolean requiresObjectInfo) {

        log.debug("getChildren");

        // skip and max
        int skip = skipCount == null ? 0 : skipCount.intValue();
        if (skip < 0) {
            skip = 0;
        }

        int max = maxItems == null ? Integer.MAX_VALUE : maxItems.intValue();
        if (max < 0) {
            max = Integer.MAX_VALUE;
        }

        // get the folder
        JcrFolder jcrFolder = getJcrNode(session, folderId).asFolder();

        // set object info of the the folder
        if (requiresObjectInfo) {
            jcrFolder.compileObjectType(null, false, objectInfos, requiresObjectInfo);
        }

        // prepare result
        ObjectInFolderListImpl result = new ObjectInFolderListImpl();
        result.setObjects(new ArrayList<ObjectInFolderData>());
        result.setHasMoreItems(false);
        int count = 0;

        // iterate through children
        Set<String> splitFilter = splitFilter(filter);
        Iterator<JcrNode> childNodes = jcrFolder.getNodes();
        while (childNodes.hasNext()) {
            JcrNode child = childNodes.next();            
            count++;

            if (skip > 0) {
                skip--;
                continue;
            }

            if (result.getObjects().size() >= max) {
                result.setHasMoreItems(true);
                continue;
            }

            // build and add child object
            ObjectInFolderDataImpl objectInFolder = new ObjectInFolderDataImpl();
            objectInFolder.setObject(child.compileObjectType(splitFilter, includeAllowableActions, objectInfos,
                    requiresObjectInfo));

            if (Boolean.TRUE.equals(includePathSegment)) {
                objectInFolder.setPathSegment(child.getName());
            }

            result.getObjects().add(objectInFolder);
        }

        result.setNumItems(BigInteger.valueOf(count));
        return result;
    }

    /**
     * See CMIS 1.0 section 2.2.3.2 getDescendants
     */
    public List<ObjectInFolderContainer> getDescendants(Session session, String folderId, BigInteger depth,
            String filter, Boolean includeAllowableActions, Boolean includePathSegment, ObjectInfoHandler objectInfos,
            boolean requiresObjectInfo, boolean foldersOnly) {

        log.debug("getDescendants or getFolderTree");

        // check depth
        int d = depth == null ? 2 : depth.intValue();
        if (d == 0) {
            throw new CmisInvalidArgumentException("Depth must not be 0!");
        }
        if (d < -1) {
            d = -1;
        }

        // get the folder
        JcrFolder jcrFolder = getJcrNode(session, folderId).asFolder();

        // set object info of the the folder
        if (requiresObjectInfo) {
            jcrFolder.compileObjectType(null, false, objectInfos, requiresObjectInfo);
        }

        // get the tree
        List<ObjectInFolderContainer> result = new ArrayList<ObjectInFolderContainer>();
        gatherDescendants(jcrFolder, result, foldersOnly, d, splitFilter(filter), includeAllowableActions,
                includePathSegment, objectInfos, requiresObjectInfo);

        return result;
    }

    /**
     * See CMIS 1.0 section 2.2.3.4 getFolderParent
     */
    public ObjectData getFolderParent(Session session, String folderId, String filter, ObjectInfoHandler objectInfos,
            boolean requiresObjectInfo) {

        List<ObjectParentData> parents = getObjectParents(session, folderId, filter, false, false, objectInfos,
                requiresObjectInfo);

        if (parents.isEmpty()) {
            throw new CmisInvalidArgumentException("The root folder has no parent!");
        }

        return parents.get(0).getObject();
    }

    /**
     * See CMIS 1.0 section 2.2.3.5 getObjectParents
     */
    public List<ObjectParentData> getObjectParents(Session session, String objectId, String filter,
            Boolean includeAllowableActions, Boolean includeRelativePathSegment, ObjectInfoHandler objectInfos,
            boolean requiresObjectInfo) {

        log.debug("getObjectParents");

        // get the file or folder
        JcrNode jcrNode = getJcrNode(session, objectId);

        // don't climb above the root folder
        if (jcrNode.isRoot()) {
            return Collections.emptyList();
        }

        // set object info of the the object
        if (requiresObjectInfo) {
            jcrNode.compileObjectType(null, false, objectInfos, requiresObjectInfo);
        }

        // get parent
        JcrNode parent = jcrNode.getParent();
        ObjectData object = parent.compileObjectType(splitFilter(filter), includeAllowableActions, objectInfos,
                requiresObjectInfo);

        ObjectParentDataImpl result = new ObjectParentDataImpl();
        result.setObject(object);
        if (Boolean.TRUE.equals(includeRelativePathSegment)) {
            result.setRelativePathSegment(parent.getName());
        }

        return Collections.singletonList((ObjectParentData) result);
    }

    /**
     * See CMIS 1.0 section 2.2.4.9 getObjectByPath
     */
    public ObjectData getObjectByPath(Session session, String folderPath, String filter, boolean includeAllowableActions,
            boolean includeACL, ObjectInfoHandler objectInfos, boolean requiresObjectInfo) {

        log.debug("getObjectByPath");

        // check path 
        if (folderPath == null || !PathManager.isAbsolute(folderPath)) {
            throw new CmisInvalidArgumentException("Invalid folder path!");
        }

        JcrNode root = getJcrNode(session, PathManager.CMIS_ROOT_ID);
        JcrNode jcrNode;
        if (PathManager.isRoot(folderPath)) {
            jcrNode = root;
        }
        else {
            String path = PathManager.relativize(PathManager.CMIS_ROOT_PATH, folderPath);
            jcrNode = root.getNode(path);
        }

        return jcrNode.compileObjectType(splitFilter(filter), includeAllowableActions, objectInfos, requiresObjectInfo);
    }

    /**
     * See CMIS 1.0 section 2.2.3.6 getCheckedOutDocs
     */
    public ObjectList getCheckedOutDocs(Session session, String folderId, String filter, String orderBy,
            Boolean includeAllowableActions, BigInteger maxItems, BigInteger skipCount) {

        log.debug("getCheckedOutDocs");

        // skip and max
        int skip = skipCount == null ? 0 : skipCount.intValue();
        if (skip < 0) {
            skip = 0;
        }

        int max = maxItems == null ? Integer.MAX_VALUE : maxItems.intValue();
        if (max < 0) {
            max = Integer.MAX_VALUE;
        }

        try {
            // Build xpath query of the form
            // '//path/to/folderId//*[jcr:isCheckedOut='true' and (not(@jcr:createdBy) or @jcr:createdBy='admin')]'
            String xPath = "/*[jcr:isCheckedOut='true' " +
                    "and (not(@jcr:createdBy) or @jcr:createdBy='" + session.getUserID() + "')]";
            
            if (folderId != null) {
                JcrFolder jcrFolder = getJcrNode(session, folderId).asFolder();
                String path = jcrFolder.getNode().getPath();
                if ("/".equals(path)) {
                    path = "";
                }
                xPath = '/' + Util.escape(path) + xPath;
            }
            else {
                xPath = '/' + xPath;
            }

            // Execute query
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(xPath, Query.XPATH);
            QueryResult queryResult = query.execute();

            // prepare results
            ObjectListImpl result = new ObjectListImpl();
            result.setObjects(new ArrayList<ObjectData>());
            result.setHasMoreItems(false);

            // iterate through children
            Set<String> splitFilter = splitFilter(filter);
            int count = 0;
            NodeIterator nodes = queryResult.getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                JcrNode jcrNode = typeHandlerManager.create(node);
                if (!jcrNode.isVersionable()) {
                    continue;
                }

                count++;

                if (skip > 0) {
                    skip--;
                    continue;
                }

                if (result.getObjects().size() >= max) {
                    result.setHasMoreItems(true);
                    continue;
                }
                
                // build and add child object
                JcrPrivateWorkingCopy jcrVersion = jcrNode.asVersion().getPwc();
                ObjectData objectData = jcrVersion.compileObjectType(splitFilter, includeAllowableActions, null, false);
                result.getObjects().add(objectData);
            }

            result.setNumItems(BigInteger.valueOf(count));
            return result;
        }
        catch (RepositoryException e) {
            log.debug(e.getMessage(), e);
            throw new CmisRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * See CMIS 1.0 section 2.2.7.1 checkOut
     */
    public void checkOut(Session session, Holder<String> objectId, Holder<Boolean> contentCopied) {
        log.debug("checkout");

        // check id 
        if (objectId == null || objectId.getValue() == null) {
            throw new CmisInvalidArgumentException("Object Id must be set.");
        }

        // get the node
        JcrNode jcrNode = getJcrNode(session, objectId.getValue());
        if (!jcrNode.isVersionable()) {
            throw new CmisUpdateConflictException("Not a version: " + jcrNode);
        }

        // checkout
        JcrPrivateWorkingCopy pwc = jcrNode.asVersion().checkout();
        objectId.setValue(pwc.getId());
        if (contentCopied != null) {
            contentCopied.setValue(true);
        }
    }

    /**
     * See CMIS 1.0 section 2.2.7.2 cancelCheckout
     */
    public void cancelCheckout(Session session, String objectId) {
        log.debug("cancelCheckout");

        // check id
        if (objectId == null) {
            throw new CmisInvalidArgumentException("Object Id must be set.");
        }

        // get the node
        JcrNode jcrNode = getJcrNode(session, objectId);
        if (!jcrNode.isVersionable()) {
            throw new CmisUpdateConflictException("Not a version: " + jcrNode);
        }

        // cancelCheckout
        jcrNode.asVersion().cancelCheckout();
    }

    /**
     * See CMIS 1.0 section 2.2.7.3 checkedIn
     */
    public void checkIn(Session session, Holder<String> objectId, Boolean major, Properties properties,
            ContentStream contentStream, String checkinComment) {

        log.debug("checkin");

        // check id
        if (objectId == null || objectId.getValue() == null) {
            throw new CmisInvalidArgumentException("Object Id must be set.");
        }

        // get the node
        JcrNode jcrNode;
        try {
            jcrNode = getJcrNode(session, objectId.getValue());
        }
        catch (CmisObjectNotFoundException e) {
            throw new CmisUpdateConflictException(e.getCause().getMessage(), e.getCause());
        }
        
        if (!jcrNode.isVersionable()) {
            throw new CmisUpdateConflictException("Not a version: " + jcrNode);
        }

        // checkin
        JcrVersion checkedIn = jcrNode.asVersion().checkin(properties, contentStream, checkinComment);
        objectId.setValue(checkedIn.getId());
    }

    /**
     * See CMIS 1.0 section 2.2.7.6 getAllVersions
     */
    public List<ObjectData> getAllVersions(Session session, String objectId, String filter,
            Boolean includeAllowableActions, ObjectInfoHandler objectInfos, boolean requiresObjectInfo) {

        log.debug("getAllVersions");

        // check id
        if (objectId == null) {
            throw new CmisInvalidArgumentException("Object Id must be set.");
        }

        Set<String> splitFilter = splitFilter(filter);

        // get the node
        JcrNode jcrNode = getJcrNode(session, objectId);

        // Collect versions
        if (jcrNode.isVersionable()) {
            JcrVersionBase jcrVersion = jcrNode.asVersion();

            Iterator<JcrVersion> versions = jcrVersion.getVersions();
            if (versions.hasNext()) {
                versions.next(); // skip root version
            }

            List<ObjectData> allVersions = new ArrayList<ObjectData>();
            while (versions.hasNext()) {
                JcrVersion version = versions.next();
                ObjectData objectData = version.compileObjectType(splitFilter, includeAllowableActions, objectInfos,
                        requiresObjectInfo);
                allVersions.add(objectData);
            }

            // Add pwc if checked out
            if (jcrVersion.isDocumentCheckedOut()) {
                JcrPrivateWorkingCopy pwc = jcrVersion.getPwc();
                ObjectData objectData = pwc.compileObjectType(splitFilter, includeAllowableActions, objectInfos,
                        requiresObjectInfo);

                allVersions.add(objectData);
            }

            // CMIS mandates descending order
            Collections.reverse(allVersions);
            return allVersions;
        }
        else {
            // Single version
            ObjectData objectData = jcrNode.compileObjectType(splitFilter, includeAllowableActions, objectInfos,
                    requiresObjectInfo);

            return Collections.singletonList(objectData);
        }

    }

    /**
     * See CMIS 1.0 section 2.2.6.1 query
     */
    public ObjectList query(final Session session, String statement, Boolean searchAllVersions,
            Boolean includeAllowableActions, BigInteger maxItems, BigInteger skipCount) {

        log.debug("query");

        if (searchAllVersions) {
            throw new CmisNotSupportedException("Not supported: query for all versions");
        }

        // skip and max
        int skip = skipCount == null ? 0 : skipCount.intValue();  
        if (skip < 0) {
            skip = 0;
        }

        int max = maxItems == null ? Integer.MAX_VALUE : maxItems.intValue();
        if (max < 0) {
            max = Integer.MAX_VALUE;
        }

        QueryTranslator queryTranslator = new QueryTranslator(typeManager) {
            @Override
            protected String jcrPathFromId(String id) {
                try {
                    JcrFolder folder = getJcrNode(session, id).asFolder();
                    String path = folder.getNode().getPath();
                    return Util.escape(path);                    
                }
                catch (RepositoryException e) {
                    log.debug(e.getMessage(), e);
                    throw new CmisRuntimeException(e.getMessage(), e);
                }
            }

            @Override
            protected String jcrPathFromCol(TypeDefinition fromType, String name) {
                return typeHandlerManager.getIdentifierMap(fromType.getId()).jcrPathFromCol(name);
            }

            @Override
            protected String jcrTypeName(TypeDefinition fromType) {
                return typeHandlerManager.getIdentifierMap(fromType.getId()).jcrTypeName();
            }

            @Override
            protected String jcrTypeCondition(TypeDefinition fromType) {
                return typeHandlerManager.getIdentifierMap(fromType.getId()).jcrTypeCondition();
            }
        };

        String xPath = queryTranslator.translateToXPath(statement);
        try {  
            // Execute query
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(xPath, Query.XPATH);

            if (skip > 0) {
                query.setOffset(skip);
            }
            if (max < Integer.MAX_VALUE) {
                query.setLimit(max + 1);    // One more in order to detect whether there are more items
            }

            QueryResult queryResult = query.execute();

            // prepare results
            ObjectListImpl result = new ObjectListImpl();
            result.setObjects(new ArrayList<ObjectData>());
            result.setHasMoreItems(false);

            // iterate through children
            int count = 0;
            NodeIterator nodes = queryResult.getNodes();
            while (nodes.hasNext() && result.getObjects().size() < max) {
                Node node = nodes.nextNode();
                JcrNode jcrNode = typeHandlerManager.create(node);
                count++;

                // Get pwc if this node is versionable and checked out
                if (jcrNode.isVersionable() && jcrNode.asVersion().isCheckedOut()) {
                    jcrNode = jcrNode.asVersion().getPwc();
                }

                // build and add child object
                ObjectData objectData = jcrNode.compileObjectType(null, includeAllowableActions, null, false);
                result.getObjects().add(objectData);
            }

            result.setHasMoreItems(nodes.hasNext());
            result.setNumItems(BigInteger.valueOf(count));
            return result;
        }
        catch (RepositoryException e) {
            log.debug(e.getMessage(), e);
            throw new CmisRuntimeException(e.getMessage(), e);
        }
    }

    //------------------------------------------< protected >---

    protected RepositoryInfo compileRepositoryInfo(String repositoryId) {
        RepositoryInfoImpl fRepositoryInfo = new RepositoryInfoImpl();

        fRepositoryInfo.setId(repositoryId);
        fRepositoryInfo.setName(getRepositoryName());
        fRepositoryInfo.setDescription(getRepositoryDescription());

        fRepositoryInfo.setCmisVersionSupported("1.0");

        fRepositoryInfo.setProductName("OpenCMIS JCR");
        fRepositoryInfo.setProductVersion("0.3");
        fRepositoryInfo.setVendorName("OpenCMIS");

        fRepositoryInfo.setRootFolder(PathManager.CMIS_ROOT_ID);
        fRepositoryInfo.setThinClientUri("");

        RepositoryCapabilitiesImpl capabilities = new RepositoryCapabilitiesImpl();
        capabilities.setCapabilityAcl(CapabilityAcl.NONE);
        capabilities.setAllVersionsSearchable(false);
        capabilities.setCapabilityJoin(CapabilityJoin.NONE);
        capabilities.setSupportsMultifiling(false);
        capabilities.setSupportsUnfiling(false);
        capabilities.setSupportsVersionSpecificFiling(false);
        capabilities.setIsPwcSearchable(false);
        capabilities.setIsPwcUpdatable(true);
        capabilities.setCapabilityQuery(CapabilityQuery.BOTHCOMBINED);
        capabilities.setCapabilityChanges(CapabilityChanges.OBJECTIDSONLY);
        capabilities.setCapabilityContentStreamUpdates(CapabilityContentStreamUpdates.ANYTIME);
        capabilities.setSupportsGetDescendants(true);
        capabilities.setSupportsGetFolderTree(true);
        capabilities.setCapabilityRendition(CapabilityRenditions.NONE);
        fRepositoryInfo.setCapabilities(capabilities);
        
        return fRepositoryInfo;
    }

    protected String getRepositoryName() {
        return repository.getDescriptor(Repository.REP_NAME_DESC);
    }

    protected String getRepositoryDescription() {
        StringBuilder description = new StringBuilder();

        for (String key : repository.getDescriptorKeys()) {
            description
                    .append(key)
                    .append('=')
                    .append(repository.getDescriptor(key))
                    .append('\n');
        }

        return description.toString();
    }

    protected JcrNode getJcrNode(Session session, String id) {
        try {
            if (id == null || id.length() == 0) {
                throw new CmisInvalidArgumentException("Null or empty id");
            }

            if (id.equals(PathManager.CMIS_ROOT_ID)) {
                return typeHandlerManager.create(getRootNode(session));
            }

            Node node = session.getNodeByIdentifier(id);
            JcrNode jcrNode = typeHandlerManager.create(node);
            
            // if node isn't under versioning, then return retrieved object 
            if (!jcrNode.isVersionable()) {
            	return jcrNode;
            }
            
            JcrVersionBase versionNode = jcrNode.asVersion();
            if (JcrPrivateWorkingCopy.denotesPwc(versionNode.getVersionLabel())) {
                return typeHandlerManager.create(versionNode.getPwc().getNode());
            }
            else {
               JcrVersion version = versionNode.getVersion(((JcrVersion) versionNode).getVersionName());
               return typeHandlerManager.create(version.getNode());
            }

        }
        catch (ItemNotFoundException e) {
            log.debug(e.getMessage(), e);
            throw new CmisObjectNotFoundException(e.getMessage(), e);
        }
        catch (RepositoryException e) {
            log.debug(e.getMessage(), e);
            throw new CmisRuntimeException(e.getMessage(), e);
        }
    }

    protected Node getRootNode(Session session) {
        try {
            return session.getNode(pathManager.getJcrRootPath());
        }
        catch (PathNotFoundException e) {
            log.debug(e.getMessage(), e);
            throw new CmisObjectNotFoundException(e.getMessage(), e);
        }
        catch (ItemNotFoundException e) {
            log.debug(e.getMessage(), e);
            throw new CmisObjectNotFoundException(e.getMessage(), e);
        }
        catch (RepositoryException e) {
            log.debug(e.getMessage(), e);
            throw new CmisRuntimeException(e.getMessage(), e);
        }
    }

    //------------------------------------------< private >---

    /**
     * Transitively gather the children of a node down to a specific depth
     */
    private static void gatherDescendants(JcrFolder jcrFolder, List<ObjectInFolderContainer> list,
            boolean foldersOnly, int depth, Set<String> filter, Boolean includeAllowableActions,
            Boolean includePathSegments, ObjectInfoHandler objectInfos, boolean requiresObjectInfo) {

        // iterate through children
        Iterator<JcrNode> childNodes = jcrFolder.getNodes();
        while (childNodes.hasNext()) {
            JcrNode child = childNodes.next();

            // folders only?
            if (foldersOnly && !child.isFolder()) {
                continue;
            }

            // add to list
            ObjectInFolderDataImpl objectInFolder = new ObjectInFolderDataImpl();
            objectInFolder.setObject(child.compileObjectType(filter, includeAllowableActions, objectInfos,
                    requiresObjectInfo));

            if (Boolean.TRUE.equals(includePathSegments)) {
                objectInFolder.setPathSegment(child.getName());
            }

            ObjectInFolderContainerImpl container = new ObjectInFolderContainerImpl();
            container.setObject(objectInFolder);

            list.add(container);

            // move to next level
            if (depth != 1 && child.isFolder()) {
                container.setChildren(new ArrayList<ObjectInFolderContainer>());
                gatherDescendants(child.asFolder(), container.getChildren(), foldersOnly, depth - 1, filter,
                        includeAllowableActions, includePathSegments, objectInfos, requiresObjectInfo);
            }
        }
    }

    /**
     * Splits a filter statement into a collection of properties.
     */
    private static Set<String> splitFilter(String filter) {
        if (filter == null) {
            return null;
        }

        if (filter.trim().length() == 0) {
            return null;
        }

        Set<String> result = new HashSet<String>();
        for (String s : filter.split(",")) {
            s = s.trim();
            if (s.equals("*")) {
                return null;
            } else if (s.length() > 0) {
                result.add(s);
            }
        }

        // set a few base properties
        // query name == id (for base type properties)
        result.add(PropertyIds.OBJECT_ID);
        result.add(PropertyIds.OBJECT_TYPE_ID);
        result.add(PropertyIds.BASE_TYPE_ID);

        return result;
    }
    
    /**
	 * Implementation of the Unified Search Discussion 
	 * (see <a>http://dev.day.com/discussion-groups/content/lists/cmis-tc/2009-02/2009-02-18__cmis_Groups__Unified_Search_Discussion__17_February_2009__20090217_Unified_Search_Discussion_pdf_u.html</a>)
	 * 
	 * @param session
	 * @param changeLogToken
	 *            the string is represented as a millisecond value of the latest
	 *            event in the list retrieved in previous call that is an offset
	 *            from the Epoch, January 1, 1970 00:00:00.000 GMT (Gregorian).
	 * @param includeProperties
	 * @param filter
	 * @param includePolicyIds
	 * @param includeAcl
	 * @param maxItems
	 * @param extension
	 * @return object list contains mockup CMIS objects
	 */
    public ObjectList getContentChanges(Session session, String changeLogToken, Boolean includeProperties,
    		String filter, Boolean includePolicyIds, Boolean includeAcl,
    		BigInteger maxItems, ExtensionsData extension) {
    	
    	try {
    		Workspace workspace = session.getWorkspace();
			EventJournal journal = workspace.getObservationManager().getEventJournal();
			
			if (null == journal || !hasCapability(session, CapabilityChanges.OBJECTIDSONLY)) {
				return null;
			}
			
			ObjectListImpl result = new ObjectListImpl();
			if (!journal.hasNext()) {
				result.setObjects(Collections.<ObjectData> emptyList());
				return result;
			}
			
			//Ensure that it isn't first page of the event list to be returned
			if (null != changeLogToken){
				long timestampToken = 0L;
				try {
					//try to parse 'changeLogToken' as a signed decimal long with assumption that one is a time stamp 
					timestampToken =  Long.parseLong(changeLogToken);
				}catch(NumberFormatException e){
					log.debug(e.getMessage(), e);
	                throw new CmisRuntimeException("Cannot parse the 'changeLogToken' argument as a signed decimal long.", e);
				}
				
				journal.skipTo(timestampToken);
			}
			List<ObjectData> objDataList = new ArrayList<ObjectData>(); 
			Event event;
			BigInteger itemsCounter = new BigInteger("0");
			do {
				event = journal.nextEvent();
				if (event.getType() == Event.PERSIST)
					continue;
				
				ObjectDataImpl objData =  new ObjectDataImpl();
				objData.setChangeEventInfo(convertToChangeEventInfo(event));
				PropertiesImpl props = new PropertiesImpl();
				//add objectId property
				props.addProperty(new PropertyIdImpl(PropertyIds.OBJECT_ID, event.getIdentifier()));
				props.addProperty(new PropertyStringImpl(PropertyIds.LAST_MODIFIED_BY, event.getUserID()));
				props.addProperty(new PropertyDateTimeImpl(PropertyIds.LAST_MODIFICATION_DATE, 
						objData.getChangeEventInfo().getChangeTime()));
				
				objData.setProperties(props);
				
				includePolicyIds(includePolicyIds, objData);
				includeAcl(includeAcl, objData);
				
				objDataList.add(objData);
				
				//increment counter
				itemsCounter = itemsCounter.add(BigInteger.ONE);
				
			} while (journal.hasNext() && !itemsCounter.equals(maxItems));
			
			result.setObjects(objDataList);
			result.setHasMoreItems(journal.hasNext());
			result.setNumItems(itemsCounter);
			return result;
			
		} catch (UnsupportedRepositoryOperationException e) {
			log.debug(e.getMessage(), e);
            throw new CmisRuntimeException(e.getMessage(), e);
		} catch (RepositoryException e) {
			log.debug(e.getMessage(), e);
            throw new CmisRuntimeException(e.getMessage(), e);
		}
    }

	private ChangeEventInfo convertToChangeEventInfo(Event event)
			throws RepositoryException {
		ChangeEventInfoDataImpl eventInfo = new ChangeEventInfoDataImpl();
		GregorianCalendar eventTime = new GregorianCalendar();
		eventTime.setTimeInMillis(event.getDate());
		eventInfo.setChangeTime(eventTime);
		eventInfo.setChangeType(convertToChangeType(event.getType()));
		return eventInfo;
	}

	private void includeAcl(Boolean includeAcl, ObjectDataImpl objData) {
		if (!includeAcl)
			return;
		
		//TODO Add ACL after JCR bridge will support it
	}

	private void includePolicyIds(Boolean includePolicyIds,
			ObjectDataImpl objData) {
		if (!includePolicyIds)
			return;
		
		//TODO Add policy list after JCR bridge will support it
	}

	private boolean hasCapability(Session session, CapabilityChanges capability) {
		return getRepositoryInfo(session).getCapabilities().getChangesCapability() == capability;
	}

    /**
     * The method converts constants of the <code>javax.jcr.observation.Event</code> type to 
     * <code>org.apache.chemistry.opencmis.commons.enums.ChangeType</code>
     * @param eventTypeConst event type
     * @return enum type of change event. 
     */
    private ChangeType convertToChangeType(int eventTypeConst){
    	switch (eventTypeConst) {
    		case Event.PROPERTY_ADDED: 
    		case Event.NODE_ADDED : 
    			return ChangeType.CREATED;
    		case Event.PROPERTY_CHANGED:
    		case Event.NODE_MOVED : 
    			return ChangeType.UPDATED;
    		case Event.PROPERTY_REMOVED:
    		case Event.NODE_REMOVED : 
    			return ChangeType.DELETED;
    		default: 
    			throw new RuntimeException("Unknown event type! Did spec change?");
    	}
    	
    }
	
    public Repository getRepository() {
        return repository;
    }

    public JcrTypeManager getTypeManager() {
        return typeManager;
    }

    public PathManager getPathManager() {
        return pathManager;
    }

    public JcrTypeHandlerManager getTypeHandlerManager() {
        return typeHandlerManager;
    }
}
