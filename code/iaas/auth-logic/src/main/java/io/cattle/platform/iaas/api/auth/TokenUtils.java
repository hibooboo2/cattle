package io.cattle.platform.iaas.api.auth;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.token.TokenException;
import io.cattle.platform.token.TokenService;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.http.Cookie;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class TokenUtils {

    public static final String ACCESSMODE = "accessMode";
    public static final String TOKEN = "token";
    public static final String ACCOUNT_ID = "account_id";
    public static final String ACCESS_TOKEN_INVALID = "InvalidAccessToken";


    @Inject
    protected AuthDao authDao;

    @Inject
    TokenService tokenService;

    public Account getAccountFromJWT() {
        Map<String, Object> jsonData = getJsonData();
        if (jsonData == null) {
            return null;
        }
        String accountId = ObjectUtils.toString(jsonData.get(ACCOUNT_ID), null);
        if (null == accountId) {
            return null;
        }
        return authDao.getAccountByExternalId(accountId, getAccountType());
    }

    protected abstract String getAccountType();

    protected Map<String, Object> getJsonData() {
        return getJsonData(getJWT(), tokenType());
    }

    protected abstract String tokenType();

    @SuppressWarnings("unchecked")
    private Map<String, Object> getJsonData(String jwt, String tokenType) {
        if (StringUtils.isEmpty(jwt)) {
            return null;
        }
        String toParse;
        String[] tokenArr = jwt.split("\\s+");
        if (tokenArr.length == 2) {
            if (!StringUtils.equalsIgnoreCase("bearer", StringUtils.trim(tokenArr[0]))) {
                return null;
            }
            toParse = tokenArr[1];
        } else if (tokenArr.length == 1) {
            toParse = tokenArr[0];
        } else {
            toParse = jwt;
        }
        Map<String, Object> jsonData;
        try {
            jsonData = tokenService.getJsonPayload(toParse, true);
        } catch (TokenException e) { // in case of invalid token
            return null;
        }
        if (jsonData == null) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, ACCESS_TOKEN_INVALID,
                    "Json Web Token invalid.", null);
        }
        String tokenTypeActual = (String) jsonData.get(TOKEN);
        if (!StringUtils.equals(tokenType, tokenTypeActual)) {
            return null;
        }
        if (!isAllowed(jsonData)) {
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }
        return jsonData;
    }

    public String getJWT() {
        ApiRequest request = ApiContext.getContext().getApiRequest();
        String jwt = (String) request.getAttribute(tokenType());
        if (StringUtils.isNotBlank(jwt) && getJsonData(jwt, tokenType()) == null) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, ValidationErrorCodes.INVALID_FORMAT,
                    "Token malformed after retrieval.", null);
        }
        return jwt;
    }

    protected abstract boolean isAllowed(Map<String, Object> jsonData);

    public Set<Identity> identities() {
        Map<String, Object> jsonData = getJsonData();
        if (jsonData == null) {
            return new HashSet<>();
        }
        return identities(jsonData);
    }

    protected abstract Set<Identity> identities(Map<String, Object> jsonData);

    public Set<Identity> getIdentities() {
        return identities();
    }

    public void findAndSetJWT() {
        ApiRequest request = ApiContext.getContext().getApiRequest();
        String jwt = (String) request.getAttribute(tokenType());
        if (StringUtils.isNotBlank(jwt) && getJsonData(jwt, tokenType()) != null) {
            return;
        }
        if (StringUtils.isBlank(jwt)) {
            for (Cookie cookie : request.getServletContext().getRequest().getCookies()) {
                if (cookie.getName().equalsIgnoreCase(TOKEN)
                        && StringUtils.isNotBlank(cookie.getValue())) {
                    jwt = cookie.getValue();
                }
            }
        }
        if (StringUtils.isBlank(jwt)) {
            jwt = request.getServletContext().getRequest().getHeader(ProjectConstants.AUTH_HEADER);
        }
        if (StringUtils.isBlank(jwt)) {
            jwt = request.getServletContext().getRequest().getParameter(TOKEN);
        }
        if (getJsonData(jwt, tokenType()) != null) {
            request.setAttribute(tokenType(), jwt);
        }
    }

    public boolean isAllowed(List<String> idList, Set<Identity> identities) {
        boolean hasAccessToAProject = authDao.hasAccessToAnyProject(identities, false, null);
        switch (accessMode()) {
            case "restricted":
                if (hasAccessToAProject || isWhitelisted(idList)) {
                    break;
                }
                throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
            case "unrestricted":
                break;
            default:
                throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }
        return true;
    }

    protected abstract boolean isWhitelisted(List<String> idList);

    protected abstract String accessMode();

    public String getAccessToken() {
        findAndSetJWT();
        return (String) DataAccessor.fields(getAccountFromJWT()).withKey(accessToken()).get();
    }

    protected abstract String accessToken();
}
