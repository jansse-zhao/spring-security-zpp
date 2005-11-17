/* Copyright 2004, 2005 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.acegisecurity.providers.dao.ldap;

import org.acegisecurity.AccountExpiredException;
import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.AuthenticationServiceException;
import org.acegisecurity.CredentialsExpiredException;
import org.acegisecurity.DisabledException;
import org.acegisecurity.LockedException;
import org.acegisecurity.UserDetails;
import org.acegisecurity.providers.AuthenticationProvider;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.acegisecurity.providers.dao.DaoAuthenticationProvider;
import org.acegisecurity.providers.dao.UserCache;
import org.acegisecurity.providers.dao.cache.NullUserCache;

import org.springframework.beans.factory.InitializingBean;

import org.springframework.dao.DataAccessException;

import org.springframework.util.Assert;


/**
 * An {@link AuthenticationProvider} implementation that retrieves user details
 * from a {@link PasswordAuthenticationDao}.
 * 
 * <p>
 * This <code>AuthenticationProvider</code> is capable of validating {@link
 * UsernamePasswordAuthenticationToken} requests containing the correct
 * username, password and when the user is not disabled.
 * </p>
 * 
 * <p>
 * Unlike {@link DaoAuthenticationProvider}, the responsibility for password
 * validation is delegated to <code>PasswordAuthenticationDao</code>.
 * </p>
 * 
 * <p>
 * Upon successful validation, a
 * <code>UsernamePasswordAuthenticationToken</code> will be created and
 * returned to the caller. The token will include as its principal either a
 * <code>String</code> representation of the username, or the {@link
 * UserDetails} that was returned from the authentication repository. Using
 * <code>String</code> is appropriate if a container adapter is being used, as
 * it expects <code>String</code> representations of the username. Using
 * <code>UserDetails</code> is appropriate if you require access to additional
 * properties of the authenticated user, such as email addresses,
 * human-friendly names etc. As container adapters are not recommended to be
 * used, and <code>UserDetails</code> implementations provide additional
 * flexibility, by default a <code>UserDetails</code> is returned. To override
 * this default, set the {@link #setForcePrincipalAsString} to
 * <code>true</code>.
 * </p>
 * 
 * <p>
 * Caching is handled via the <code>UserDetails</code> object being placed in
 * the {@link UserCache}. This ensures that subsequent requests with the same
 * username and password can be validated without needing to query the {@link
 * PasswordAuthenticationDao}. It should be noted that if a user appears to
 * present an incorrect password, the {@link PasswordAuthenticationDao} will
 * be queried to confirm the most up-to-date password was used for comparison.
 * </p>
 * 
 * <p>
 * If an application context is detected (which is automatically the case when
 * the bean is started within a Spring container), application events will be
 * published to the context. See {@link
 * org.acegisecurity.event.authentication.AbstractAuthenticationEvent} for
 * further information.
 * </p>
 *
 * @deprecated instead subclass {@link org.acegisecurity.providers.dao.AbstractUserDetailsAuthenticationProvider}
 * @author Karel Miarka
 */
public class PasswordDaoAuthenticationProvider implements AuthenticationProvider,
    InitializingBean {
    //~ Instance fields ========================================================

    private PasswordAuthenticationDao authenticationDao;
    private UserCache userCache = new NullUserCache();
    private boolean forcePrincipalAsString = false;

    //~ Methods ================================================================

    public void setForcePrincipalAsString(boolean forcePrincipalAsString) {
        this.forcePrincipalAsString = forcePrincipalAsString;
    }

    public boolean isForcePrincipalAsString() {
        return forcePrincipalAsString;
    }

    public void setPasswordAuthenticationDao(
        PasswordAuthenticationDao authenticationDao) {
        this.authenticationDao = authenticationDao;
    }

    public PasswordAuthenticationDao getPasswordAuthenticationDao() {
        return authenticationDao;
    }

    public void setUserCache(UserCache userCache) {
        this.userCache = userCache;
    }

    public UserCache getUserCache() {
        return userCache;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.authenticationDao,
            "A Password authentication DAO must be set");
        Assert.notNull(this.userCache, "A user cache must be set");
    }

    public Authentication authenticate(Authentication authentication)
        throws AuthenticationException {
        // Determine username
        String username = authentication.getPrincipal().toString();

        if (authentication.getPrincipal() instanceof UserDetails) {
            username = ((UserDetails) authentication.getPrincipal())
                .getUsername();
        }

        String password = authentication.getCredentials().toString();

        boolean cacheWasUsed = true;
        UserDetails user = this.userCache.getUserFromCache(username);

        // Check if the provided password is the same as the password in cache
        if ((user != null) && !password.equals(user.getPassword())) {
            user = null;
            this.userCache.removeUserFromCache(username);
        }

        if (user == null) {
            cacheWasUsed = false;
            user = getUserFromBackend(username, password);
        }

        if (!user.isEnabled()) {
            throw new DisabledException("User is disabled");
        }

        if (!user.isAccountNonExpired()) {
            throw new AccountExpiredException("User account has expired");
        }

        if (!user.isAccountNonLocked()) {
            throw new LockedException("User account is locked");
        }

        if (!user.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException(
                "User credentials have expired");
        }

        if (!cacheWasUsed) {
            // Put into cache
            this.userCache.putUserInCache(user);
        }

        Object principalToReturn = user;

        if (forcePrincipalAsString) {
            principalToReturn = user.getUsername();
        }

        return createSuccessAuthentication(principalToReturn, authentication,
            user);
    }

    public boolean supports(Class authentication) {
        if (UsernamePasswordAuthenticationToken.class.isAssignableFrom(
                authentication)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Creates a successful {@link Authentication} object.
     * 
     * <P>
     * Protected so subclasses can override. This might be required if multiple
     * credentials need to be placed into a custom <code>Authentication</code>
     * object, such as a password as well as a ZIP code.
     * </p>
     * 
     * <P>
     * Subclasses will usually store the original credentials the user supplied
     * (not salted or encoded passwords) in the returned
     * <code>Authentication</code> object.
     * </p>
     *
     * @param principal that should be the principal in the returned object
     *        (defined by the {@link #isForcePrincipalAsString()} method)
     * @param authentication that was presented to the
     *        <code>PasswordDaoAuthenticationProvider</code> for validation
     * @param user that was loaded by the
     *        <code>PasswordAuthenticationDao</code>
     *
     * @return the successful authentication token
     */
    protected Authentication createSuccessAuthentication(Object principal,
        Authentication authentication, UserDetails user) {
        // Ensure we return the original credentials the user supplied,
        // so subsequent attempts are successful even with encoded passwords.
        // Also ensure we return the original getDetails(), so that future
        // authentication events after cache expiry contain the details
        UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(principal,
                authentication.getCredentials(), user.getAuthorities());
        result.setDetails((authentication.getDetails() != null)
            ? authentication.getDetails() : null);

        return result;
    }

    private UserDetails getUserFromBackend(String username, String password) {
        try {
            return this.authenticationDao.loadUserByUsernameAndPassword(username,
                password);
        } catch (DataAccessException repositoryProblem) {
            throw new AuthenticationServiceException(repositoryProblem
                .getMessage(), repositoryProblem);
        }
    }
}
