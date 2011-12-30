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
package org.apache.chemistry.opencmis.commons.impl;

import static org.apache.chemistry.opencmis.commons.impl.JSONConstants.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AclCapabilities;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ChangeEventInfo;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.PermissionMapping;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyBoolean;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.PropertyDateTime;
import org.apache.chemistry.opencmis.commons.data.PropertyDecimal;
import org.apache.chemistry.opencmis.commons.data.PropertyHtml;
import org.apache.chemistry.opencmis.commons.data.PropertyId;
import org.apache.chemistry.opencmis.commons.data.PropertyInteger;
import org.apache.chemistry.opencmis.commons.data.PropertyString;
import org.apache.chemistry.opencmis.commons.data.PropertyUri;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.data.RepositoryCapabilities;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.Choice;
import org.apache.chemistry.opencmis.commons.definitions.DocumentTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PermissionDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyBooleanDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDateTimeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDecimalDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyHtmlDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyIdDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyIntegerDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyStringDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyUriDefinition;
import org.apache.chemistry.opencmis.commons.definitions.RelationshipTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.CapabilityAcl;
import org.apache.chemistry.opencmis.commons.enums.CapabilityChanges;
import org.apache.chemistry.opencmis.commons.enums.CapabilityContentStreamUpdates;
import org.apache.chemistry.opencmis.commons.enums.CapabilityJoin;
import org.apache.chemistry.opencmis.commons.enums.CapabilityQuery;
import org.apache.chemistry.opencmis.commons.enums.CapabilityRenditions;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.ContentStreamAllowed;
import org.apache.chemistry.opencmis.commons.enums.DateTimeResolution;
import org.apache.chemistry.opencmis.commons.enums.DecimalPrecision;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.SupportedPermissions;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractPropertyDefinition;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractTypeDefinition;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AclCapabilitiesDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ChoiceImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.CmisExtensionElementImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.DocumentTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FolderTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PermissionDefinitionDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PermissionMappingDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PolicyTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyHtmlDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyUriDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RelationshipTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryCapabilitiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryInfoBrowserBindingImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionContainerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionListImpl;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * OpenCMIS objects to JSON converter.
 */
public class JSONConverter {

    /**
     * Private constructor.
     */
    private JSONConverter() {
    }

    /**
     * Converts a repository info object.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject convert(RepositoryInfo repositoryInfo, String repositoryUrl, String rootUrl) {
        if (repositoryInfo == null) {
            return null;
        }

        JSONObject result = new JSONObject();

        result.put(JSON_REPINFO_ID, repositoryInfo.getId());
        result.put(JSON_REPINFO_NAME, repositoryInfo.getName());
        result.put(JSON_REPINFO_DESCRIPTION, repositoryInfo.getDescription());
        result.put(JSON_REPINFO_VENDOR, repositoryInfo.getVendorName());
        result.put(JSON_REPINFO_PRODUCT, repositoryInfo.getProductName());
        result.put(JSON_REPINFO_PRODUCT_VERSION, repositoryInfo.getProductVersion());
        result.put(JSON_REPINFO_ROOT_FOLDER_ID, repositoryInfo.getRootFolderId());
        result.put(JSON_REPINFO_CAPABILITIES, convert(repositoryInfo.getCapabilities()));
        setIfNotNull(JSON_REPINFO_ACL_CAPABILITIES, convert(repositoryInfo.getAclCapabilities()), result);
        result.put(JSON_REPINFO_CHANGE_LOCK_TOKEN, repositoryInfo.getLatestChangeLogToken());
        result.put(JSON_REPINFO_CMIS_VERSION_SUPPORTED, repositoryInfo.getCmisVersionSupported());
        setIfNotNull(JSON_REPINFO_THIN_CLIENT_URI, repositoryInfo.getThinClientUri(), result);
        setIfNotNull(JSON_REPINFO_CHANGES_INCOMPLETE, repositoryInfo.getChangesIncomplete(), result);

        if (repositoryInfo.getChangesOnType() != null) {
            JSONArray changesOnType = new JSONArray();

            for (BaseTypeId type : repositoryInfo.getChangesOnType()) {
                changesOnType.add(getJSONStringValue(type.value()));
            }

            result.put(JSON_REPINFO_CHANGES_ON_TYPE, changesOnType);
        }

        setIfNotNull(JSON_REPINFO_PRINCIPAL_ID_ANONYMOUS, repositoryInfo.getPrincipalIdAnonymous(), result);
        setIfNotNull(JSON_REPINFO_PRINCIPAL_ID_ANYONE, repositoryInfo.getPrincipalIdAnyone(), result);

        result.put(JSON_REPINFO_REPOSITORY_URL, repositoryUrl);
        result.put(JSON_REPINFO_ROOT_FOLDER_URL, rootUrl);

        convertExtension(repositoryInfo, result);

        return result;
    }

    /**
     * Converts a capabilities object.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject convert(RepositoryCapabilities capabilities) {
        if (capabilities == null) {
            return null;
        }

        JSONObject result = new JSONObject();

        result.put(JSON_CAP_CONTENT_STREAM_UPDATES, getJSONStringValue(capabilities.getContentStreamUpdatesCapability()
                .value()));
        result.put(JSON_CAP_CHANGES, getJSONStringValue(capabilities.getChangesCapability().value()));
        result.put(JSON_CAP_RENDITIONS, getJSONStringValue(capabilities.getRenditionsCapability().value()));
        result.put(JSON_CAP_GET_DESCENDANTS, capabilities.isGetDescendantsSupported());
        result.put(JSON_CAP_GET_FOLDER_TREE, capabilities.isGetFolderTreeSupported());
        result.put(JSON_CAP_MULTIFILING, capabilities.isMultifilingSupported());
        result.put(JSON_CAP_UNFILING, capabilities.isUnfilingSupported());
        result.put(JSON_CAP_VERSION_SPECIFIC_FILING, capabilities.isVersionSpecificFilingSupported());
        result.put(JSON_CAP_PWC_SEARCHABLE, capabilities.isPwcSearchableSupported());
        result.put(JSON_CAP_PWC_UPDATABLE, capabilities.isPwcUpdatableSupported());
        result.put(JSON_CAP_ALL_VERSIONS_SEARCHABLE, capabilities.isAllVersionsSearchableSupported());
        result.put(JSON_CAP_QUERY, getJSONStringValue(capabilities.getQueryCapability().value()));
        result.put(JSON_CAP_JOIN, getJSONStringValue(capabilities.getJoinCapability().value()));
        result.put(JSON_CAP_ACL, getJSONStringValue(capabilities.getAclCapability().value()));

        convertExtension(capabilities, result);

        return result;
    }

    /**
     * Converts an ACL capabilities object.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject convert(AclCapabilities capabilities) {
        if (capabilities == null) {
            return null;
        }

        JSONObject result = new JSONObject();

        result.put(JSON_ACLCAP_SUPPORTED_PERMISSIONS,
                getJSONStringValue(capabilities.getSupportedPermissions().value()));
        result.put(JSON_ACLCAP_ACL_PROPAGATION, getJSONStringValue(capabilities.getAclPropagation().value()));

        // permissions
        if (capabilities.getPermissions() != null) {
            JSONArray permissions = new JSONArray();

            for (PermissionDefinition permDef : capabilities.getPermissions()) {
                JSONObject permission = new JSONObject();
                permission.put(JSON_ACLCAP_PERMISSION_PERMISSION, permDef.getId());
                permission.put(JSON_ACLCAP_PERMISSION_DESCRIPTION, permDef.getDescription());

                permissions.add(permission);
            }

            result.put(JSON_ACLCAP_PERMISSIONS, permissions);
        }

        // permission mapping
        if (capabilities.getPermissionMapping() != null) {
            JSONArray permissionMapping = new JSONArray();

            for (PermissionMapping permMap : capabilities.getPermissionMapping().values()) {
                JSONArray mappingPermissions = new JSONArray();
                if (permMap.getPermissions() != null) {
                    for (String p : permMap.getPermissions()) {
                        mappingPermissions.add(p);
                    }
                }

                JSONObject mapping = new JSONObject();
                mapping.put(JSON_ACLCAP_MAPPING_KEY, permMap.getKey());
                mapping.put(JSON_ACLCAP_MAPPING_PERMISSION, mappingPermissions);

                permissionMapping.add(mapping);
            }

            result.put(JSON_ACLCAP_PERMISSION_MAPPING, permissionMapping);
        }

        convertExtension(capabilities, result);

        return result;
    }

    @SuppressWarnings("unchecked")
    public static RepositoryInfo convertRepositoryInfo(Map<String, Object> json) {
        if (json == null) {
            return null;
        }

        RepositoryInfoBrowserBindingImpl result = new RepositoryInfoBrowserBindingImpl();

        result.setId(getString(json, JSON_REPINFO_ID));
        result.setName(getString(json, JSON_REPINFO_NAME));
        result.setDescription(getString(json, JSON_REPINFO_DESCRIPTION));
        result.setVendorName(getString(json, JSON_REPINFO_VENDOR));
        result.setProductName(getString(json, JSON_REPINFO_PRODUCT));
        result.setProductVersion(getString(json, JSON_REPINFO_PRODUCT_VERSION));
        result.setRootFolder(getString(json, JSON_REPINFO_ROOT_FOLDER_ID));
        result.setRepositoryUrl(getString(json, JSON_REPINFO_REPOSITORY_URL));
        result.setRootUrl(getString(json, JSON_REPINFO_ROOT_FOLDER_URL));

        Object capabilities = json.get(JSON_REPINFO_CAPABILITIES);
        if (capabilities instanceof Map) {
            result.setCapabilities(convertRepositoryCapabilities((Map<String, Object>) capabilities));
        }

        Object aclCapabilities = json.get(JSON_REPINFO_ACL_CAPABILITIES);
        if (aclCapabilities instanceof Map) {
            result.setAclCapabilities(convertAclCapabilities((Map<String, Object>) aclCapabilities));
        }

        result.setLatestChangeLogToken(getString(json, JSON_REPINFO_CHANGE_LOCK_TOKEN));
        result.setCmisVersionSupported(getString(json, JSON_REPINFO_CMIS_VERSION_SUPPORTED));
        result.setThinClientUri(getString(json, JSON_REPINFO_THIN_CLIENT_URI));
        result.setChangesIncomplete(getBoolean(json, JSON_REPINFO_CHANGES_INCOMPLETE));

        Object changesOnType = json.get(JSON_REPINFO_CHANGES_ON_TYPE);
        if (changesOnType instanceof List) {
            List<BaseTypeId> types = new ArrayList<BaseTypeId>();
            for (Object type : ((List<Object>) changesOnType)) {
                if (type != null) {
                    types.add(BaseTypeId.fromValue(type.toString()));
                }
            }

            result.setChangesOnType(types);
        }

        result.setPrincipalAnonymous(getString(json, JSON_REPINFO_PRINCIPAL_ID_ANONYMOUS));
        result.setPrincipalAnyone(getString(json, JSON_REPINFO_PRINCIPAL_ID_ANYONE));

        // handle extensions
        convertExtension(json, result, REPINFO_KEYS);

        return result;
    }

    public static RepositoryCapabilities convertRepositoryCapabilities(Map<String, Object> json) {
        if (json == null) {
            return null;
        }

        RepositoryCapabilitiesImpl result = new RepositoryCapabilitiesImpl();

        result.setCapabilityContentStreamUpdates(getEnum(json, JSON_CAP_CONTENT_STREAM_UPDATES,
                CapabilityContentStreamUpdates.class));
        result.setCapabilityChanges(getEnum(json, JSON_CAP_CHANGES, CapabilityChanges.class));
        result.setCapabilityRendition(getEnum(json, JSON_CAP_RENDITIONS, CapabilityRenditions.class));
        result.setSupportsGetDescendants(getBoolean(json, JSON_CAP_GET_DESCENDANTS));
        result.setSupportsGetFolderTree(getBoolean(json, JSON_CAP_GET_FOLDER_TREE));
        result.setSupportsMultifiling(getBoolean(json, JSON_CAP_MULTIFILING));
        result.setSupportsUnfiling(getBoolean(json, JSON_CAP_UNFILING));
        result.setSupportsVersionSpecificFiling(getBoolean(json, JSON_CAP_VERSION_SPECIFIC_FILING));
        result.setIsPwcSearchable(getBoolean(json, JSON_CAP_PWC_SEARCHABLE));
        result.setIsPwcUpdatable(getBoolean(json, JSON_CAP_PWC_UPDATABLE));
        result.setAllVersionsSearchable(getBoolean(json, JSON_CAP_ALL_VERSIONS_SEARCHABLE));
        result.setCapabilityQuery(getEnum(json, JSON_CAP_QUERY, CapabilityQuery.class));
        result.setCapabilityJoin(getEnum(json, JSON_CAP_JOIN, CapabilityJoin.class));
        result.setCapabilityAcl(getEnum(json, JSON_CAP_ACL, CapabilityAcl.class));

        // handle extensions
        convertExtension(json, result, CAP_KEYS);

        return result;
    }

    @SuppressWarnings("unchecked")
    public static AclCapabilities convertAclCapabilities(Map<String, Object> json) {
        if (json == null) {
            return null;
        }

        AclCapabilitiesDataImpl result = new AclCapabilitiesDataImpl();

        result.setSupportedPermissions(getEnum(json, JSON_ACLCAP_SUPPORTED_PERMISSIONS, SupportedPermissions.class));
        result.setAclPropagation(getEnum(json, JSON_ACLCAP_ACL_PROPAGATION, AclPropagation.class));

        Object permissions = json.get(JSON_ACLCAP_PERMISSIONS);
        if (permissions instanceof List) {
            List<PermissionDefinition> permissionDefinitionList = new ArrayList<PermissionDefinition>();

            for (Object permission : (List<Object>) permissions) {
                if (permission instanceof Map) {
                    PermissionDefinitionDataImpl permDef = new PermissionDefinitionDataImpl();

                    permDef.setPermission(getString((Map<String, Object>) permission, JSON_ACLCAP_PERMISSION_PERMISSION));
                    permDef.setDescription(getString((Map<String, Object>) permission,
                            JSON_ACLCAP_PERMISSION_DESCRIPTION));

                    convertExtension((Map<String, Object>) permission, permDef, ACLCAP_PERMISSION_KEYS);

                    permissionDefinitionList.add(permDef);
                }
            }

            result.setPermissionDefinitionData(permissionDefinitionList);
        }

        Object permissionMapping = json.get(JSON_ACLCAP_PERMISSION_MAPPING);
        if (permissionMapping instanceof List) {
            Map<String, PermissionMapping> permMap = new HashMap<String, PermissionMapping>();

            for (Object permission : (List<Object>) permissionMapping) {
                if (permission instanceof Map) {
                    PermissionMappingDataImpl mapping = new PermissionMappingDataImpl();

                    String key = getString((Map<String, Object>) permission, JSON_ACLCAP_MAPPING_KEY);
                    mapping.setKey(key);

                    Object perms = ((Map<String, Object>) permission).get(JSON_ACLCAP_MAPPING_PERMISSION);
                    if (perms instanceof List) {
                        List<String> permList = new ArrayList<String>();

                        for (Object perm : (List<Object>) perms) {
                            if (perm != null) {
                                permList.add(perm.toString());
                            }
                        }

                        mapping.setPermissions(permList);
                    }

                    convertExtension((Map<String, Object>) permission, mapping, ACLCAP_MAPPING_KEYS);

                    permMap.put(key, mapping);
                }
            }

            result.setPermissionMappingData(permMap);
        }

        // handle extensions
        convertExtension(json, result, ACLCAP_KEYS);

        return result;
    }

    @SuppressWarnings("unchecked")
    public static TypeDefinition convertTypeDefinition(Map<String, Object> json) {
        if (json == null) {
            return null;
        }

        AbstractTypeDefinition result = null;

        String id = getString(json, JSON_TYPE_ID);

        // find base type
        BaseTypeId baseType = getEnum(json, JSON_TYPE_BASE_ID, BaseTypeId.class);
        if (baseType == null) {
            throw new CmisConnectionException("Invalid base type: " + id);
        }

        switch (baseType) {
        case CMIS_FOLDER:
            result = new FolderTypeDefinitionImpl();
            break;
        case CMIS_DOCUMENT:
            result = new DocumentTypeDefinitionImpl();

            ((DocumentTypeDefinitionImpl) result).setContentStreamAllowed(getEnum(json,
                    JSON_TYPE_CONTENTSTREAM_ALLOWED, ContentStreamAllowed.class));
            ((DocumentTypeDefinitionImpl) result).setIsVersionable(getBoolean(json, JSON_TYPE_VERSIONABLE));

            break;
        case CMIS_RELATIONSHIP:
            result = new RelationshipTypeDefinitionImpl();

            Object allowedSourceTypes = json.get(JSON_TYPE_ALLOWED_SOURCE_TYPES);
            if (allowedSourceTypes instanceof List) {
                List<String> types = new ArrayList<String>();
                for (Object type : ((List<Object>) allowedSourceTypes)) {
                    if (type != null) {
                        types.add(type.toString());
                    }
                }

                ((RelationshipTypeDefinitionImpl) result).setAllowedSourceTypes(types);
            }

            Object allowedTargetTypes = json.get(JSON_TYPE_ALLOWED_TARGET_TYPES);
            if (allowedTargetTypes instanceof List) {
                List<String> types = new ArrayList<String>();
                for (Object type : ((List<Object>) allowedTargetTypes)) {
                    if (type != null) {
                        types.add(type.toString());
                    }
                }

                ((RelationshipTypeDefinitionImpl) result).setAllowedTargetTypes(types);
            }

            break;
        case CMIS_POLICY:
            result = new PolicyTypeDefinitionImpl();
            break;
        default:
            throw new CmisRuntimeException("Type '" + id + "' does not match a base type!");
        }

        result.setBaseTypeId(baseType);
        result.setDescription(getString(json, JSON_TYPE_DESCRIPTION));
        result.setDisplayName(getString(json, JSON_TYPE_DISPLAYNAME));
        result.setId(id);
        result.setIsControllableAcl(getBoolean(json, JSON_TYPE_CONTROLABLE_ACL));
        result.setIsControllablePolicy(getBoolean(json, JSON_TYPE_CONTROLABLE_POLICY));
        result.setIsCreatable(getBoolean(json, JSON_TYPE_CREATABLE));
        result.setIsFileable(getBoolean(json, JSON_TYPE_FILEABLE));
        result.setIsFulltextIndexed(getBoolean(json, JSON_TYPE_FULLTEXT_INDEXED));
        result.setIsIncludedInSupertypeQuery(getBoolean(json, JSON_TYPE_INCLUDE_IN_SUPERTYPE_QUERY));
        result.setIsQueryable(getBoolean(json, JSON_TYPE_QUERYABLE));
        result.setLocalName(getString(json, JSON_TYPE_LOCALNAME));
        result.setLocalNamespace(getString(json, JSON_TYPE_LOCALNAMESPACE));
        result.setParentTypeId(getString(json, JSON_TYPE_PARENT_ID));
        result.setQueryName(getString(json, JSON_TYPE_QUERYNAME));

        Object propertyDefinitions = json.get(JSON_TYPE_PROPERTY_DEFINITIONS);
        if (propertyDefinitions instanceof Map) {
            for (Object propDef : ((Map<String, Object>) propertyDefinitions).values()) {
                if (propDef instanceof Map) {
                    result.addPropertyDefinition(convertPropertyDefinition((Map<String, Object>) propDef));
                }
            }
        }

        // handle extensions
        convertExtension(json, result, TYPE_KEYS);

        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static PropertyDefinition<?> convertPropertyDefinition(Map<String, Object> json) {
        if (json == null) {
            return null;
        }

        AbstractPropertyDefinition<?> result = null;

        String id = getString(json, JSON_PROPERTY_ID);

        // find property type
        PropertyType propertyType = getEnum(json, JSON_PROPERTY_TYPE_PROPERTY_TYPE, PropertyType.class);
        if (propertyType == null) {
            throw new CmisRuntimeException("Invalid property type '" + id + "'! Data type not set!");
        }

        // find
        Cardinality cardinality = getEnum(json, JSON_PROPERTY_TYPE_CARDINALITY, Cardinality.class);
        if (cardinality == null) {
            throw new CmisRuntimeException("Invalid property type '" + id + "'! Cardinality not set!");
        }

        switch (propertyType) {
        case STRING:
            result = new PropertyStringDefinitionImpl();
            ((PropertyStringDefinitionImpl) result).setMaxLength(getInteger(json, JSON_PROPERTY_TYPE_MAX_LENGTH));
            ((PropertyStringDefinitionImpl) result)
                    .setChoices(convertChoicesString(json.get(JSON_PROPERTY_TYPE_CHOICE)));
            break;
        case ID:
            result = new PropertyIdDefinitionImpl();
            ((PropertyIdDefinitionImpl) result).setChoices(convertChoicesString(json.get(JSON_PROPERTY_TYPE_CHOICE)));
            break;
        case BOOLEAN:
            result = new PropertyBooleanDefinitionImpl();
            ((PropertyBooleanDefinitionImpl) result).setChoices(convertChoicesBoolean(json
                    .get(JSON_PROPERTY_TYPE_CHOICE)));
            break;
        case INTEGER:
            result = new PropertyIntegerDefinitionImpl();
            ((PropertyIntegerDefinitionImpl) result).setMinValue(getInteger(json, JSON_PROPERTY_TYPE_MIN_VALUE));
            ((PropertyIntegerDefinitionImpl) result).setMaxValue(getInteger(json, JSON_PROPERTY_TYPE_MAX_VALUE));
            ((PropertyIntegerDefinitionImpl) result).setChoices(convertChoicesInteger(json
                    .get(JSON_PROPERTY_TYPE_CHOICE)));
            break;
        case DATETIME:
            result = new PropertyDateTimeDefinitionImpl();
            ((PropertyDateTimeDefinitionImpl) result).setDateTimeResolution(getEnum(json,
                    JSON_PROPERTY_TYPE_RESOLUTION, DateTimeResolution.class));
            ((PropertyDateTimeDefinitionImpl) result).setChoices(convertChoicesDateTime(json
                    .get(JSON_PROPERTY_TYPE_CHOICE)));
            break;
        case DECIMAL:
            result = new PropertyDecimalDefinitionImpl();
            ((PropertyDecimalDefinitionImpl) result).setMinValue(getDecimal(json, JSON_PROPERTY_TYPE_MIN_VALUE));
            ((PropertyDecimalDefinitionImpl) result).setMaxValue(getDecimal(json, JSON_PROPERTY_TYPE_MAX_VALUE));
            ((PropertyDecimalDefinitionImpl) result).setPrecision(getEnum(json, JSON_PROPERTY_TYPE_PRECISION,
                    DecimalPrecision.class));
            ((PropertyDecimalDefinitionImpl) result).setChoices(convertChoicesDecimal(json
                    .get(JSON_PROPERTY_TYPE_CHOICE)));
            break;
        case HTML:
            result = new PropertyHtmlDefinitionImpl();
            ((PropertyHtmlDefinitionImpl) result).setChoices(convertChoicesString(json.get(JSON_PROPERTY_TYPE_CHOICE)));
            break;
        case URI:
            result = new PropertyUriDefinitionImpl();
            ((PropertyUriDefinitionImpl) result).setChoices(convertChoicesString(json.get(JSON_PROPERTY_TYPE_CHOICE)));
            break;
        default:
            throw new CmisRuntimeException("Property type '" + id + "' does not match a data type!");
        }

        // default value
        Object defaultValue = json.get(JSON_PROPERTY_TYPE_DEAULT_VALUE);
        if (defaultValue != null) {
            if (defaultValue instanceof List) {
                List values = new ArrayList();
                for (Object value : (List) defaultValue) {
                    values.add(getCMISValue(value, propertyType));
                }
                result.setDefaultValue(values);
            } else {
                result.setDefaultValue((List) Collections.singletonList(getCMISValue(defaultValue, propertyType)));
            }
        }

        // generic
        result.setId(id);
        result.setPropertyType(propertyType);
        result.setCardinality(cardinality);
        result.setLocalName(getString(json, JSON_PROPERTY_TYPE_LOCALNAME));
        result.setLocalNamespace(getString(json, JSON_PROPERTY_TYPE_LOCALNAMESPACE));
        result.setQueryName(getString(json, JSON_PROPERTY_TYPE_QUERYNAME));
        result.setDescription(getString(json, JSON_PROPERTY_TYPE_DESCRIPTION));
        result.setDisplayName(getString(json, JSON_PROPERTY_TYPE_DISPLAYNAME));
        result.setIsInherited(getBoolean(json, JSON_PROPERTY_TYPE_INHERITED));
        result.setIsOpenChoice(getBoolean(json, JSON_PROPERTY_TYPE_OPENCHOICE));
        result.setIsOrderable(getBoolean(json, JSON_PROPERTY_TYPE_ORDERABLE));
        result.setIsQueryable(getBoolean(json, JSON_PROPERTY_TYPE_QUERYABLE));
        result.setIsRequired(getBoolean(json, JSON_PROPERTY_TYPE_REQUIRED));
        result.setUpdatability(getEnum(json, JSON_PROPERTY_TYPE_UPDATABILITY, Updatability.class));

        // handle extensions
        convertExtension(json, result, PROPERTY_TYPE_KEYS);

        return result;
    }

    /**
     * Converts choices.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<Choice<String>> convertChoicesString(Object choices) {
        if (!(choices instanceof List)) {
            return null;
        }

        List<Choice<String>> result = new ArrayList<Choice<String>>();

        for (Object obj : (List) choices) {
            if (obj instanceof Map) {
                Map<String, Object> choiceObj = (Map<String, Object>) obj;

                ChoiceImpl<String> choice = new ChoiceImpl<String>();
                choice.setDisplayName(getString(choiceObj, JSON_PROPERTY_TYPE_CHOICE_DISPLAYNAME));

                Object choiceValue = choiceObj.get(JSON_PROPERTY_TYPE_CHOICE_VALUE);
                List<String> values = new ArrayList<String>();
                if (choiceValue instanceof List) {
                    for (Object value : (List) choiceValue) {
                        values.add((String) getCMISValue(value, PropertyType.STRING));
                    }
                } else {
                    values.add((String) getCMISValue(choiceValue, PropertyType.STRING));
                }
                choice.setValue(values);

                choice.setChoice(convertChoicesString(choiceObj.get(JSON_PROPERTY_TYPE_CHOICE_CHOICE)));

                result.add(choice);
            }
        }

        return result;
    }

    /**
     * Converts choices.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<Choice<Boolean>> convertChoicesBoolean(Object choices) {
        if (!(choices instanceof List)) {
            return null;
        }

        List<Choice<Boolean>> result = new ArrayList<Choice<Boolean>>();

        for (Object obj : (List) choices) {
            if (obj instanceof Map) {
                Map<String, Object> choiceObj = (Map<String, Object>) obj;

                ChoiceImpl<Boolean> choice = new ChoiceImpl<Boolean>();
                choice.setDisplayName(getString(choiceObj, JSON_PROPERTY_TYPE_CHOICE_DISPLAYNAME));

                Object choiceValue = choiceObj.get(JSON_PROPERTY_TYPE_CHOICE_VALUE);
                List<Boolean> values = new ArrayList<Boolean>();
                if (choiceValue instanceof List) {
                    for (Object value : (List) choiceValue) {
                        values.add((Boolean) getCMISValue(value, PropertyType.BOOLEAN));
                    }
                } else {
                    values.add((Boolean) getCMISValue(choiceValue, PropertyType.BOOLEAN));
                }
                choice.setValue(values);

                choice.setChoice(convertChoicesBoolean(choiceObj.get(JSON_PROPERTY_TYPE_CHOICE_CHOICE)));

                result.add(choice);
            }
        }

        return result;
    }

    /**
     * Converts choices.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<Choice<BigInteger>> convertChoicesInteger(Object choices) {
        if (!(choices instanceof List)) {
            return null;
        }

        List<Choice<BigInteger>> result = new ArrayList<Choice<BigInteger>>();

        for (Object obj : (List) choices) {
            if (obj instanceof Map) {
                Map<String, Object> choiceObj = (Map<String, Object>) obj;

                ChoiceImpl<BigInteger> choice = new ChoiceImpl<BigInteger>();
                choice.setDisplayName(getString(choiceObj, JSON_PROPERTY_TYPE_CHOICE_DISPLAYNAME));

                Object choiceValue = choiceObj.get(JSON_PROPERTY_TYPE_CHOICE_VALUE);
                List<BigInteger> values = new ArrayList<BigInteger>();
                if (choiceValue instanceof List) {
                    for (Object value : (List) choiceValue) {
                        values.add((BigInteger) getCMISValue(value, PropertyType.INTEGER));
                    }
                } else {
                    values.add((BigInteger) getCMISValue(choiceValue, PropertyType.INTEGER));
                }
                choice.setValue(values);

                choice.setChoice(convertChoicesInteger(choiceObj.get(JSON_PROPERTY_TYPE_CHOICE_CHOICE)));

                result.add(choice);
            }
        }

        return result;
    }

    /**
     * Converts choices.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<Choice<BigDecimal>> convertChoicesDecimal(Object choices) {
        if (!(choices instanceof List)) {
            return null;
        }

        List<Choice<BigDecimal>> result = new ArrayList<Choice<BigDecimal>>();

        for (Object obj : (List) choices) {
            if (obj instanceof Map) {
                Map<String, Object> choiceObj = (Map<String, Object>) obj;

                ChoiceImpl<BigDecimal> choice = new ChoiceImpl<BigDecimal>();
                choice.setDisplayName(getString(choiceObj, JSON_PROPERTY_TYPE_CHOICE_DISPLAYNAME));

                Object choiceValue = choiceObj.get(JSON_PROPERTY_TYPE_CHOICE_VALUE);
                List<BigDecimal> values = new ArrayList<BigDecimal>();
                if (choiceValue instanceof List) {
                    for (Object value : (List) choiceValue) {
                        values.add((BigDecimal) getCMISValue(value, PropertyType.DECIMAL));
                    }
                } else {
                    values.add((BigDecimal) getCMISValue(choiceValue, PropertyType.DECIMAL));
                }
                choice.setValue(values);

                choice.setChoice(convertChoicesDecimal(choiceObj.get(JSON_PROPERTY_TYPE_CHOICE_CHOICE)));

                result.add(choice);
            }
        }

        return result;
    }

    /**
     * Converts choices.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<Choice<GregorianCalendar>> convertChoicesDateTime(Object choices) {
        if (!(choices instanceof List)) {
            return null;
        }

        List<Choice<GregorianCalendar>> result = new ArrayList<Choice<GregorianCalendar>>();

        for (Object obj : (List) choices) {
            if (obj instanceof Map) {
                Map<String, Object> choiceObj = (Map<String, Object>) obj;

                ChoiceImpl<GregorianCalendar> choice = new ChoiceImpl<GregorianCalendar>();
                choice.setDisplayName(getString(choiceObj, JSON_PROPERTY_TYPE_CHOICE_DISPLAYNAME));

                Object choiceValue = choiceObj.get(JSON_PROPERTY_TYPE_CHOICE_VALUE);
                List<GregorianCalendar> values = new ArrayList<GregorianCalendar>();
                if (choiceValue instanceof List) {
                    for (Object value : (List) choiceValue) {
                        values.add((GregorianCalendar) getCMISValue(value, PropertyType.DATETIME));
                    }
                } else {
                    values.add((GregorianCalendar) getCMISValue(choiceValue, PropertyType.DATETIME));
                }
                choice.setValue(values);

                choice.setChoice(convertChoicesDateTime(choiceObj.get(JSON_PROPERTY_TYPE_CHOICE_CHOICE)));

                result.add(choice);
            }
        }

        return result;
    }

    /**
     * Converts an object.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject convert(ObjectData object, TypeCache typeCache) {
        if (object == null) {
            return null;
        }

        JSONObject result = new JSONObject();

        // properties
        if (object.getProperties() != null) {
            JSONObject properties = convert(object.getProperties(), object.getId(), typeCache);
            if (properties != null) {
                result.put(JSON_OBJECT_PROPERTIES, properties);
            }
        }

        // allowable actions
        if (object.getAllowableActions() != null) {
            result.put(JSON_OBJECT_ALLOWABLE_ACTIONS, convert(object.getAllowableActions()));
        }

        // relationships
        if (object.getRelationships() != null) {
            JSONArray relationships = new JSONArray();

            for (ObjectData relationship : object.getRelationships()) {
                relationships.add(convert(relationship, typeCache));
            }

            result.put(JSON_OBJECT_RELATIONSHIPS, relationships);
        }

        // change event info
        if (object.getChangeEventInfo() != null) {
            JSONObject changeEventInfo = new JSONObject();

            ChangeEventInfo cei = object.getChangeEventInfo();
            changeEventInfo.put(JSON_CHANGE_EVENT_TYPE, getJSONStringValue(cei.getChangeType().value()));
            changeEventInfo.put(JSON_CHANGE_EVENT_TIME, getJSONValue(cei.getChangeTime()));

            result.put(JSON_OBJECT_CHANGE_EVENT_INFO, changeEventInfo);
        }

        // ACL
        if ((object.getAcl() != null) && (object.getAcl().getAces() != null)) {
            result.put(JSON_OBJECT_ACL, convert(object.getAcl()));
            result.put(JSON_OBJECT_EXACT_ACL, object.isExactAcl());
        }

        // policy ids
        if ((object.getPolicyIds() != null) && (object.getPolicyIds().getPolicyIds() != null)) {
            JSONArray policyIds = new JSONArray();

            for (String pi : object.getPolicyIds().getPolicyIds()) {
                policyIds.add(pi);
            }

            result.put(JSON_OBJECT_POLICY_IDS, policyIds);
        }

        // renditions
        if (object.getRenditions() != null) {
            JSONArray renditions = new JSONArray();

            for (RenditionData rendition : object.getRenditions()) {
                renditions.add(convert(rendition));
            }

            result.put(JSON_OBJECT_RENDITIONS, renditions);
        }

        convertExtension(object, result);

        return result;
    }

    /**
     * Converts a bag of properties.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject convert(Properties properties, String objectId, TypeCache typeCache) {
        if (properties == null) {
            return null;
        }

        JSONObject result = new JSONObject();

        for (PropertyData<?> property : properties.getPropertyList()) {
            TypeDefinition type = null;
            if (typeCache != null) {
                type = typeCache.getTypeDefinitionForObject(objectId);
            }

            PropertyDefinition<?> propDef = null;
            if (type != null) {
                propDef = type.getPropertyDefinitions().get(property.getId());
            }

            result.put(property.getId(), convert(property, propDef));
        }

        convertExtension(properties, result);

        return result;
    }

    /**
     * Converts a property.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject convert(PropertyData<?> property, PropertyDefinition<?> propDef) {
        if (property == null) {
            return null;
        }

        JSONObject result = new JSONObject();

        result.put(JSON_PROPERTY_ID, property.getId());
        setIfNotNull(JSON_PROPERTY_LOCALNAME, property.getLocalName(), result);
        setIfNotNull(JSON_PROPERTY_DISPLAYNAME, property.getDisplayName(), result);
        setIfNotNull(JSON_PROPERTY_QUERYNAME, property.getQueryName(), result);

        if (propDef != null) {
            result.put(JSON_PROPERTY_DATATYPE, propDef.getPropertyType().value());
            result.put(JSON_PROPERTY_CARDINALITY, propDef.getCardinality().value());

            if ((property.getValues() == null) || (property.getValues().size() == 0)) {
                result.put(JSON_PROPERTY_VALUE, null);
            } else if (propDef.getCardinality() == Cardinality.SINGLE) {
                result.put(JSON_PROPERTY_VALUE, getJSONValue(property.getValues().get(0)));
            } else {
                JSONArray values = new JSONArray();

                for (Object value : property.getValues()) {
                    values.add(getJSONValue(value));
                }

                result.put(JSON_PROPERTY_VALUE, values);
            }
        } else {
            result.put(JSON_PROPERTY_DATATYPE, getJSONPropertyDataType(property));

            if ((property.getValues() == null) || (property.getValues().size() == 0)) {
                result.put(JSON_PROPERTY_VALUE, null);
            } else if (property.getValues().size() > 0) {
                JSONArray values = new JSONArray();

                for (Object value : property.getValues()) {
                    values.add(getJSONValue(value));
                }

                result.put(JSON_PROPERTY_VALUE, values);
            }
        }

        convertExtension(property, result);

        return result;
    }

    /**
     * Converts allowable actions.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject convert(AllowableActions allowableActions) {
        if (allowableActions == null) {
            return null;
        }

        JSONObject result = new JSONObject();

        Set<Action> actionSet = allowableActions.getAllowableActions();
        for (Action action : Action.values()) {
            result.put(action.value(), actionSet.contains(action));
        }

        convertExtension(allowableActions, result);

        return result;
    }

    /**
     * Converts an ACL.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject convert(Acl acl) {
        if ((acl == null) || (acl.getAces() == null)) {
            return null;
        }

        JSONArray aceObjects = new JSONArray();

        for (Ace ace : acl.getAces()) {
            JSONArray permissions = new JSONArray();
            if (ace.getPermissions() != null) {
                for (String p : ace.getPermissions()) {
                    permissions.add(p);
                }
            }

            JSONObject aceObject = new JSONObject();
            JSONObject principalObjecy = new JSONObject();
            principalObjecy.put(JSON_ACE_PRINCIPAL_ID, ace.getPrincipalId());
            aceObject.put(JSON_ACE_PRINCIPAL, principalObjecy);
            aceObject.put(JSON_ACE_PERMISSIONS, permissions);
            aceObject.put(JSON_ACE_IS_DIRECT, ace.isDirect());

            aceObjects.add(aceObject);
        }

        JSONObject result = new JSONObject();
        result.put(JSON_ACL_ACES, aceObjects);
        setIfNotNull(JSON_ACL_IS_EXACT, acl.isExact(), result);

        convertExtension(acl, result);

        return result;
    }

    /**
     * Converts a rendition.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject convert(RenditionData rendition) {
        if (rendition == null) {
            return null;
        }

        JSONObject result = new JSONObject();

        result.put(JSON_RENDITION_STREAM_ID, rendition.getStreamId());
        result.put(JSON_RENDITION_MIMETYPE, rendition.getMimeType());
        result.put(JSON_RENDITION_LENGTH, rendition.getBigLength());
        result.put(JSON_RENDITION_KIND, rendition.getKind());
        setIfNotNull(JSON_RENDITION_TITLE, rendition.getTitle(), result);
        setIfNotNull(JSON_RENDITION_HEIGHT, rendition.getBigHeight(), result);
        setIfNotNull(JSON_RENDITION_WIDTH, rendition.getBigWidth(), result);
        setIfNotNull(JSON_RENDITION_DOCUMENT_ID, rendition.getRenditionDocumentId(), result);

        convertExtension(rendition, result);

        return result;
    }

    /**
     * Converts a query object list.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject convert(ObjectList list, TypeCache typeCache) {
        if (list == null) {
            return null;
        }

        JSONObject result = new JSONObject();

        JSONArray objects = new JSONArray();
        if (list.getObjects() != null) {
            for (ObjectData object : list.getObjects()) {
                objects.add(convert(object, typeCache));
            }
        }

        result.put(JSON_OBJECTLIST_OBJECTS, objects);

        setIfNotNull(JSON_OBJECTLIST_HAS_MORE_ITEMS, list.hasMoreItems(), result);
        setIfNotNull(JSON_OBJECTLIST_NUM_ITEMS, list.getNumItems(), result);

        convertExtension(list, result);

        return result;
    }

    /**
     * Converts an object in a folder list.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject convert(ObjectInFolderData objectInFolder, TypeCache typeCache) {
        if ((objectInFolder == null) || (objectInFolder.getObject() == null)) {
            return null;
        }

        JSONObject result = new JSONObject();
        result.put(JSON_OBJECTINFOLDER_OBJECT, convert(objectInFolder.getObject(), typeCache));
        setIfNotNull(JSON_OBJECTINFOLDER_PATH_SEGMENT, objectInFolder.getPathSegment(), result);

        convertExtension(objectInFolder, result);

        return result;
    }

    /**
     * Converts a folder list.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject convert(ObjectInFolderList objectInFolderList, TypeCache typeCache) {
        if (objectInFolderList == null) {
            return null;
        }

        JSONObject result = new JSONObject();

        if (objectInFolderList.getObjects() != null) {
            JSONArray objects = new JSONArray();

            for (ObjectInFolderData object : objectInFolderList.getObjects()) {
                objects.add(convert(object, typeCache));
            }

            result.put(JSON_OBJECTINFOLDERLIST_OBJECTS, objects);
        }

        setIfNotNull(JSON_OBJECTINFOLDERLIST_HAS_MORE_ITEMS, objectInFolderList.hasMoreItems(), result);
        setIfNotNull(JSON_OBJECTINFOLDERLIST_NUM_ITEMS, objectInFolderList.getNumItems(), result);

        convertExtension(objectInFolderList, result);

        return result;
    }

    /**
     * Converts a folder container.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject convert(ObjectInFolderContainer container, TypeCache typeCache) {
        if (container == null) {
            return null;
        }

        JSONObject result = new JSONObject();
        result.put(JSON_OBJECTINFOLDERCONTAINER_OBJECT, convert(container.getObject(), typeCache));

        if ((container.getChildren() != null) && (container.getChildren().size() > 0)) {
            JSONArray children = new JSONArray();
            for (ObjectInFolderContainer descendant : container.getChildren()) {
                children.add(JSONConverter.convert(descendant, typeCache));
            }

            result.put(JSON_OBJECTINFOLDERCONTAINER_CHILDREN, children);
        }

        convertExtension(container, result);

        return result;
    }

    /**
     * Converts an object parent.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject convert(ObjectParentData parent, TypeCache typeCache) {
        if ((parent == null) || (parent.getObject() == null)) {
            return null;
        }

        JSONObject result = new JSONObject();
        result.put(JSON_OBJECTPARENTS_OBJECT, convert(parent.getObject(), typeCache));
        if (parent.getRelativePathSegment() != null) {
            result.put(JSON_OBJECTPARENTS_RELATIVE_PATH_SEGMENT, parent.getRelativePathSegment());
        }

        convertExtension(parent, result);

        return result;
    }

    /**
     * Converts a type definition.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject convert(TypeDefinition type) {
        if (type == null) {
            return null;
        }

        JSONObject result = new JSONObject();
        result.put(JSON_TYPE_ID, type.getId());
        result.put(JSON_TYPE_LOCALNAME, type.getLocalName());
        result.put(JSON_TYPE_LOCALNAMESPACE, type.getLocalNamespace());
        setIfNotNull(JSON_TYPE_DISPLAYNAME, type.getDisplayName(), result);
        setIfNotNull(JSON_TYPE_QUERYNAME, type.getQueryName(), result);
        setIfNotNull(JSON_TYPE_DESCRIPTION, type.getDescription(), result);
        result.put(JSON_TYPE_BASE_ID, type.getBaseTypeId().value());
        setIfNotNull(JSON_TYPE_PARENT_ID, type.getParentTypeId(), result);
        result.put(JSON_TYPE_CREATABLE, type.isCreatable());
        result.put(JSON_TYPE_FILEABLE, type.isFileable());
        result.put(JSON_TYPE_QUERYABLE, type.isQueryable());
        result.put(JSON_TYPE_FULLTEXT_INDEXED, type.isFulltextIndexed());
        result.put(JSON_TYPE_INCLUDE_IN_SUPERTYPE_QUERY, type.isIncludedInSupertypeQuery());
        result.put(JSON_TYPE_CONTROLABLE_POLICY, type.isControllablePolicy());
        result.put(JSON_TYPE_CONTROLABLE_ACL, type.isControllableAcl());

        if (type instanceof DocumentTypeDefinition) {
            result.put(JSON_TYPE_VERSIONABLE, ((DocumentTypeDefinition) type).isVersionable());
            result.put(JSON_TYPE_CONTENTSTREAM_ALLOWED, ((DocumentTypeDefinition) type).getContentStreamAllowed()
                    .value());
        }

        if (type instanceof RelationshipTypeDefinition) {
            result.put(JSON_TYPE_ALLOWED_SOURCE_TYPES,
                    getJSONArrayFromList(((RelationshipTypeDefinition) type).getAllowedSourceTypeIds()));
            result.put(JSON_TYPE_ALLOWED_TARGET_TYPES,
                    getJSONArrayFromList(((RelationshipTypeDefinition) type).getAllowedTargetTypeIds()));
        }

        if ((type.getPropertyDefinitions() != null) && (!type.getPropertyDefinitions().isEmpty())) {
            JSONObject propertyDefs = new JSONObject();

            for (PropertyDefinition<?> pd : type.getPropertyDefinitions().values()) {
                propertyDefs.put(pd.getId(), convert(pd));
            }

            result.put(JSON_TYPE_PROPERTY_DEFINITIONS, propertyDefs);
        }

        convertExtension(type, result);

        return result;
    }

    /**
     * Converts a property type definition.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject convert(PropertyDefinition<?> propertyDefinition) {
        if (propertyDefinition == null) {
            return null;
        }

        JSONObject result = new JSONObject();

        // type specific
        if (propertyDefinition instanceof PropertyStringDefinition) {
            setIfNotNull(JSON_PROPERTY_TYPE_MAX_LENGTH, ((PropertyStringDefinition) propertyDefinition).getMaxLength(),
                    result);
        } else if (propertyDefinition instanceof PropertyIdDefinition) {
        } else if (propertyDefinition instanceof PropertyIntegerDefinition) {
            setIfNotNull(JSON_PROPERTY_TYPE_MIN_VALUE, ((PropertyIntegerDefinition) propertyDefinition).getMinValue(),
                    result);
            setIfNotNull(JSON_PROPERTY_TYPE_MAX_VALUE, ((PropertyIntegerDefinition) propertyDefinition).getMaxValue(),
                    result);
        } else if (propertyDefinition instanceof PropertyDecimalDefinition) {
            setIfNotNull(JSON_PROPERTY_TYPE_MIN_VALUE, ((PropertyDecimalDefinition) propertyDefinition).getMinValue(),
                    result);
            setIfNotNull(JSON_PROPERTY_TYPE_MAX_VALUE, ((PropertyDecimalDefinition) propertyDefinition).getMaxValue(),
                    result);
            DecimalPrecision precision = ((PropertyDecimalDefinition) propertyDefinition).getPrecision();
            if (precision != null) {
                result.put(JSON_PROPERTY_TYPE_PRECISION, precision.value());
            }
        } else if (propertyDefinition instanceof PropertyBooleanDefinition) {
        } else if (propertyDefinition instanceof PropertyDateTimeDefinition) {
            DateTimeResolution resolution = ((PropertyDateTimeDefinition) propertyDefinition).getDateTimeResolution();
            if (resolution != null) {
                result.put(JSON_PROPERTY_TYPE_RESOLUTION, resolution.value());
            }
        } else if (propertyDefinition instanceof PropertyHtmlDefinition) {
        } else if (propertyDefinition instanceof PropertyUriDefinition) {
        }

        // default value
        if (propertyDefinition.getDefaultValue() != null) {
            if (propertyDefinition.getCardinality() == Cardinality.SINGLE) {
                if (!propertyDefinition.getDefaultValue().isEmpty()) {
                    result.put(JSON_PROPERTY_TYPE_DEAULT_VALUE, getJSONValue(propertyDefinition.getDefaultValue()
                            .get(0)));
                }
            } else {
                JSONArray values = new JSONArray();
                for (Object value : propertyDefinition.getDefaultValue()) {
                    values.add(getJSONValue(value));
                }
                result.put(JSON_PROPERTY_TYPE_DEAULT_VALUE, values);
            }
        }

        // choices
        if (propertyDefinition.getChoices() != null && !propertyDefinition.getChoices().isEmpty()) {
            result.put(JSON_PROPERTY_TYPE_CHOICE,
                    convertChoices(propertyDefinition.getChoices(), propertyDefinition.getCardinality()));
        }

        // generic
        result.put(JSON_PROPERTY_TYPE_ID, propertyDefinition.getId());
        result.put(JSON_PROPERTY_TYPE_LOCALNAME, propertyDefinition.getLocalName());
        setIfNotNull(JSON_PROPERTY_TYPE_LOCALNAMESPACE, propertyDefinition.getLocalNamespace(), result);
        setIfNotNull(JSON_PROPERTY_TYPE_DISPLAYNAME, propertyDefinition.getDisplayName(), result);
        setIfNotNull(JSON_PROPERTY_TYPE_QUERYNAME, propertyDefinition.getQueryName(), result);
        setIfNotNull(JSON_PROPERTY_TYPE_DESCRIPTION, propertyDefinition.getDescription(), result);
        result.put(JSON_PROPERTY_TYPE_PROPERTY_TYPE, propertyDefinition.getPropertyType().value());
        result.put(JSON_PROPERTY_TYPE_CARDINALITY, propertyDefinition.getCardinality().value());
        result.put(JSON_PROPERTY_TYPE_UPDATABILITY, propertyDefinition.getUpdatability().value());
        setIfNotNull(JSON_PROPERTY_TYPE_INHERITED, propertyDefinition.isInherited(), result);
        result.put(JSON_PROPERTY_TYPE_REQUIRED, propertyDefinition.isRequired());
        result.put(JSON_PROPERTY_TYPE_QUERYABLE, propertyDefinition.isQueryable());
        result.put(JSON_PROPERTY_TYPE_ORDERABLE, propertyDefinition.isOrderable());
        setIfNotNull(JSON_PROPERTY_TYPE_OPENCHOICE, propertyDefinition.isOpenChoice(), result);

        convertExtension(propertyDefinition, result);

        return result;
    }

    /**
     * Converts choices.
     */
    @SuppressWarnings("unchecked")
    private static <T> JSONArray convertChoices(List<Choice<T>> choices, Cardinality cardinality) {
        if (choices == null) {
            return null;
        }

        JSONArray result = new JSONArray();

        for (Choice<?> choice : choices) {
            JSONObject jsonChoice = new JSONObject();

            jsonChoice.put(JSON_PROPERTY_TYPE_CHOICE_DISPLAYNAME, choice.getDisplayName());

            if (cardinality == Cardinality.SINGLE) {
                if (!choice.getValue().isEmpty()) {
                    jsonChoice.put(JSON_PROPERTY_TYPE_CHOICE_VALUE, getJSONValue(choice.getValue().get(0)));
                }
            } else {
                JSONArray values = new JSONArray();
                for (Object value : choice.getValue()) {
                    values.add(getJSONValue(value));
                }
                jsonChoice.put(JSON_PROPERTY_TYPE_CHOICE_VALUE, values);
            }

            if (choice.getChoice() != null && !choice.getChoice().isEmpty()) {
                jsonChoice.put(JSON_PROPERTY_TYPE_CHOICE_CHOICE, convertChoices(choice.getChoice(), cardinality));
            }

            result.add(jsonChoice);
        }

        return result;
    }

    /**
     * Converts a type definition list.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject convert(TypeDefinitionList list) {
        if (list == null) {
            return null;
        }

        JSONObject result = new JSONObject();

        if (list.getList() != null) {
            JSONArray objects = new JSONArray();

            for (TypeDefinition type : list.getList()) {
                objects.add(convert(type));
            }

            result.put(JSON_TYPESLIST_TYPES, objects);
        }

        setIfNotNull(JSON_TYPESLIST_HAS_MORE_ITEMS, list.hasMoreItems(), result);
        setIfNotNull(JSON_TYPESLIST_NUM_ITEMS, list.getNumItems(), result);

        convertExtension(list, result);

        return result;
    }

    /**
     * Converts a type definition list.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static TypeDefinitionList convertTypeChildren(Map<String, Object> json) {
        if (json == null) {
            return null;
        }

        TypeDefinitionListImpl result = new TypeDefinitionListImpl();

        Object typesList = json.get(JSON_TYPESLIST_TYPES);
        if (typesList instanceof List) {
            List<TypeDefinition> list = new ArrayList<TypeDefinition>();

            for (Object type : (List) typesList) {
                if (type instanceof Map) {
                    list.add(convertTypeDefinition((Map<String, Object>) type));
                }
            }

            result.setList(list);
        }

        result.setHasMoreItems(getBoolean(json, JSON_TYPESLIST_HAS_MORE_ITEMS));
        result.setNumItems(getInteger(json, JSON_TYPESLIST_NUM_ITEMS));

        convertExtension(json, result, TYPESLIST_KEYS);

        return result;
    }

    /**
     * Converts a type definition container.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject convert(TypeDefinitionContainer container) {
        if (container == null) {
            return null;
        }

        JSONObject result = new JSONObject();
        result.put(JSON_TYPESCONTAINER_TYPE, convert(container.getTypeDefinition()));

        if ((container.getChildren() != null) && (container.getChildren().size() > 0)) {
            JSONArray children = new JSONArray();
            for (TypeDefinitionContainer child : container.getChildren()) {
                children.add(JSONConverter.convert(child));
            }

            result.put(JSON_TYPESCONTAINER_CHILDREN, children);
        }

        convertExtension(container, result);

        return result;
    }

    /**
     * Converts a type definition list.
     */
    @SuppressWarnings({ "unchecked" })
    public static List<TypeDefinitionContainer> convertTypeDescendants(List<Object> json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        List<TypeDefinitionContainer> result = new ArrayList<TypeDefinitionContainer>();

        for (Object obj : json) {
            if (obj instanceof Map) {
                Map<String, Object> jsonContainer = (Map<String, Object>) obj;
                TypeDefinitionContainerImpl container = new TypeDefinitionContainerImpl();

                Object type = jsonContainer.get(JSON_TYPESCONTAINER_TYPE);
                if (type instanceof Map) {
                    container.setTypeDefinition(convertTypeDefinition((Map<String, Object>) jsonContainer));
                }

                Object children = jsonContainer.get(JSON_TYPESCONTAINER_CHILDREN);
                if (children instanceof List) {
                    container.setChildren(convertTypeDescendants((List<Object>) children));
                }

                convertExtension(jsonContainer, container, TYPESCONTAINER_KEYS);

                result.add(container);
            }
        }

        return result;
    }

    // -----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static void convertExtension(ExtensionsData source, JSONObject target) {
        if (source == null || source.getExtensions() == null) {
            return;
        }

        for (CmisExtensionElement ext : source.getExtensions()) {
            if (ext.getChildren() != null && !ext.getChildren().isEmpty()) {
                target.put(ext.getName(), convertExtensionList(ext.getChildren()));
            } else {
                target.put(ext.getName(), ext.getValue());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static JSONObject convertExtensionList(List<CmisExtensionElement> extensionList) {
        if (extensionList == null) {
            return null;
        }

        JSONObject result = new JSONObject();

        for (CmisExtensionElement ext : extensionList) {
            if (ext.getChildren() != null && !ext.getChildren().isEmpty()) {
                result.put(ext.getName(), convertExtensionList(ext.getChildren()));
            } else {
                result.put(ext.getName(), ext.getValue());
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public static void convertExtension(Map<String, Object> source, ExtensionsData target, Set<String> cmisKeys) {
        if (source == null) {
            return;
        }

        List<CmisExtensionElement> extensions = null;

        for (Map.Entry<String, Object> element : source.entrySet()) {
            if (cmisKeys.contains(element.getKey())) {
                continue;
            }

            if (extensions == null) {
                extensions = new ArrayList<CmisExtensionElement>();
            }

            if (element.getValue() instanceof Map) {
                extensions.add(new CmisExtensionElementImpl(null, element.getKey(), null,
                        convertExtension((Map<String, Object>) element.getValue())));
            } else if (element.getValue() instanceof List) {
                extensions.add(new CmisExtensionElementImpl(null, element.getKey(), null,
                        convertExtension((List<Object>) element.getValue())));
            } else {
                String value = (element.getValue() == null ? null : element.getValue().toString());
                extensions.add(new CmisExtensionElementImpl(null, element.getKey(), null, value));
            }
        }

        target.setExtensions(extensions);
    }

    @SuppressWarnings("unchecked")
    public static List<CmisExtensionElement> convertExtension(Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        List<CmisExtensionElement> extensions = new ArrayList<CmisExtensionElement>();

        for (Map.Entry<String, Object> element : map.entrySet()) {
            if (element.getValue() instanceof Map) {
                extensions.add(new CmisExtensionElementImpl(null, element.getKey(), null,
                        convertExtension((Map<String, Object>) element.getValue())));
            } else if (element.getValue() instanceof List) {
                extensions.add(new CmisExtensionElementImpl(null, element.getKey(), null,
                        convertExtension((List<Object>) element.getValue())));
            } else {
                String value = (element.getValue() == null ? null : element.getValue().toString());
                extensions.add(new CmisExtensionElementImpl(null, element.getKey(), null, value));
            }
        }

        return extensions;
    }

    @SuppressWarnings("unchecked")
    public static List<CmisExtensionElement> convertExtension(List<Object> list) {
        if (list == null) {
            return null;
        }

        List<CmisExtensionElement> extensions = new ArrayList<CmisExtensionElement>();

        int i = 0;
        for (Object element : list) {
            if (element instanceof Map) {
                extensions.add(new CmisExtensionElementImpl(null, "" + i, null,
                        convertExtension((Map<String, Object>) element)));
            } else if (element instanceof List) {
                extensions.add(new CmisExtensionElementImpl(null, "" + i, null,
                        convertExtension((List<Object>) element)));
            } else {
                String value = (element == null ? null : element.toString());
                extensions.add(new CmisExtensionElementImpl(null, "" + i, null, value));
            }

            i++;
        }

        return extensions;
    }

    // -----------------------------------------------------------------

    public static String getJSONStringValue(Object obj) {
        if (obj == null) {
            return null;
        }

        return obj.toString();
    }

    public static Object getJSONValue(Object value) {
        if (value instanceof GregorianCalendar) {
            return ((GregorianCalendar) value).getTimeInMillis();
        }

        return value;
    }

    public static Object getCMISValue(Object value, PropertyType propertyType) {
        if (value == null) {
            return null;
        }

        switch (propertyType) {
        case STRING:
        case ID:
        case HTML:
        case URI:
            if (value instanceof String) {
                return value;
            }
            throw new CmisRuntimeException("Invalid String value!");
        case BOOLEAN:
            if (value instanceof Boolean) {
                return value;
            }
            throw new CmisRuntimeException("Invalid Boolean value!");
        case INTEGER:
            if (value instanceof Number) {
                return BigInteger.valueOf(((Number) value).longValue());
            }
            throw new CmisRuntimeException("Invalid Integer value!");
        case DECIMAL:
            if (value instanceof Double) {
                return BigDecimal.valueOf((Double) value);
            } else if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).longValue());
            }
            throw new CmisRuntimeException("Invalid Decimal value!");
        case DATETIME:
            if (value instanceof Number) {
                GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                cal.setTimeInMillis(((Number) value).longValue());
                return cal;
            }
            throw new CmisRuntimeException("Invalid DateTime value!");
        }

        throw new CmisRuntimeException("Unkown property type!");
    }

    @SuppressWarnings("unchecked")
    public static JSONArray getJSONArrayFromList(List<?> list) {
        if (list == null) {
            return null;
        }

        JSONArray result = new JSONArray();
        result.addAll(list);

        return result;
    }

    public static String getJSONPropertyDataType(PropertyData<?> property) {
        if (property instanceof PropertyBoolean) {
            return PropertyType.BOOLEAN.value();
        } else if (property instanceof PropertyId) {
            return PropertyType.ID.value();
        } else if (property instanceof PropertyInteger) {
            return PropertyType.INTEGER.value();
        } else if (property instanceof PropertyDateTime) {
            return PropertyType.DATETIME.value();
        } else if (property instanceof PropertyDecimal) {
            return PropertyType.DECIMAL.value();
        } else if (property instanceof PropertyHtml) {
            return PropertyType.HTML.value();
        } else if (property instanceof PropertyString) {
            return PropertyType.STRING.value();
        } else if (property instanceof PropertyUri) {
            return PropertyType.URI.value();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static void setIfNotNull(String name, Object obj, JSONObject json) {
        if (obj != null) {
            json.put(name, obj);
        }
    }

    public static String getString(@SuppressWarnings("rawtypes") Map json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : obj.toString();
    }

    public static Boolean getBoolean(@SuppressWarnings("rawtypes") Map json, String key) {
        Object obj = json.get(key);

        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }

        return null;
    }

    public static BigInteger getInteger(@SuppressWarnings("rawtypes") Map json, String key) {
        Object obj = json.get(key);

        if (obj instanceof Number) {
            return BigInteger.valueOf(((Number) obj).longValue());
        }

        return null;
    }

    public static BigDecimal getDecimal(@SuppressWarnings("rawtypes") Map json, String key) {
        Object obj = json.get(key);

        if (obj instanceof Double) {
            return BigDecimal.valueOf((Double) obj);
        } else if (obj instanceof Number) {
            return BigDecimal.valueOf(((Number) obj).longValue());
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T getEnum(@SuppressWarnings("rawtypes") Map json, String key, Class<T> clazz) {
        String value = getString(json, key);
        if (value == null) {
            return null;
        }

        try {
            Method m = clazz.getMethod("fromValue", String.class);
            return (T) m.invoke(null, value);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                return null;
            } else {
                throw new CmisRuntimeException("Could not parse enum value!", e);
            }
        }
    }
}
