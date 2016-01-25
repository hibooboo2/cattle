package io.cattle.platform.iaas.api.filter.dynamicSchema;

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

public class DynamicSchemaRoleFilter extends AbstractResourceManagerFilter {

    private static final String DEFINITION_FIELD = "definition";

    @Inject
    DynamicSchemaDao dynamicSchemaDao;

    @Inject
    JsonMapper jsonMapper;

    @SuppressWarnings("unchecked")
    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Map<String, Object> requestObject = (Map<String, Object>) request.getRequestObject();
        if (!dynamicSchemaDao.isUnique(String.valueOf(requestObject.get("name")),
                (List<String>) requestObject.get("roles"))) {
            throw new ValidationErrorException(ValidationErrorCodes.NOT_UNIQUE, "SchemaExistsForGivenRole");
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
