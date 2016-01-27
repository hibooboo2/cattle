package io.cattle.platform.iaas.api.filter.dynamicSchema;

import static io.cattle.platform.object.meta.ObjectMetaDataManager.ACCOUNT_FIELD;

import io.cattle.platform.core.dao.DynamicSchemaDao;
import io.cattle.platform.core.model.DynamicSchema;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractResourceManagerFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;

public class DynamicSchemaFilter extends AbstractResourceManagerFilter {

    private static final String DEFINITION_FIELD = "definition";

    @Inject
    DynamicSchemaDao dynamicSchemaDao;

    @Inject
    JsonMapper jsonMapper;

    @SuppressWarnings("unchecked")
    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Map<String, Object> requestObject = (Map<String, Object>) request.getRequestObject();
        if (requestObject.get(ACCOUNT_FIELD) == null) {
            requestObject.put(ACCOUNT_FIELD, null);
        }
        Long accountId = requestObject.get(ACCOUNT_FIELD) == null ? null : Long.valueOf(String.valueOf(requestObject.get(ACCOUNT_FIELD)));
        List<String> roles = (List<String>) requestObject.get("roles");
        if ((roles == null || roles.isEmpty()) && accountId == null) {
            throw new ValidationErrorException(ValidationErrorCodes.MISSING_REQUIRED, "MustSpecifyAccountIdOrRole");
        }
        if (!dynamicSchemaDao.isUnique(String.valueOf(requestObject.get("name")), roles, accountId)) {
            throw new ValidationErrorException(ValidationErrorCodes.NOT_UNIQUE, "SchemaExistsForGivenRoleAndOrId");
        }

        try {
            jsonMapper.readValue(
                    String.valueOf(requestObject.get(DEFINITION_FIELD)).getBytes("UTF-8"), SchemaImpl.class);
        } catch (Exception e) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_FORMAT, DEFINITION_FIELD);
        }
        return super.create(type, request, next);
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { DynamicSchema.class };
    }
}
