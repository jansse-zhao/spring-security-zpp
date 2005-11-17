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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import org.acegisecurity.AccountExpiredException;
import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationServiceException;
import org.acegisecurity.BadCredentialsException;
import org.acegisecurity.CredentialsExpiredException;
import org.acegisecurity.DisabledException;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.LockedException;
import org.acegisecurity.UserDetails;
import org.acegisecurity.providers.TestingAuthenticationToken;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.acegisecurity.providers.dao.User;
import org.acegisecurity.providers.dao.UserCache;
import org.acegisecurity.providers.dao.UsernameNotFoundException;
import org.acegisecurity.providers.dao.cache.EhCacheBasedUserCache;
import org.acegisecurity.providers.dao.cache.NullUserCache;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;


/**
 * Tests {@link PasswordDaoAuthenticationProvider}.
 *
 * @author Karel Miarka
 */
public class PasswordDaoAuthenticationProviderTests extends TestCase {
    //~ Methods ================================================================

    public final void setUp() throws Exception {
        super.setUp();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PasswordDaoAuthenticationProviderTests.class);
    }

    public void testAuthenticateFailsForIncorrectPasswordCase() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("marissa",
                "KOala");

        PasswordDaoAuthenticationProvider provider = new PasswordDaoAuthenticationProvider();
        provider.setPasswordAuthenticationDao(new MockAuthenticationDaoUserMarissa());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown BadCredentialsException");
        } catch (BadCredentialsException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticateFailsIfAccountExpired() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("peter",
                "opal");

        PasswordDaoAuthenticationProvider provider = new PasswordDaoAuthenticationProvider();
        provider.setPasswordAuthenticationDao(new MockAuthenticationDaoUserPeterAccountExpired());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown AccountExpiredException");
        } catch (AccountExpiredException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticateFailsIfAccountLocked() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("peter",
                "opal");

        PasswordDaoAuthenticationProvider provider = new PasswordDaoAuthenticationProvider();
        provider.setPasswordAuthenticationDao(new MockAuthenticationDaoUserPeterAccountLocked());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown AccountExpiredException");
        } catch (LockedException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticateFailsIfCredentialsExpired() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("peter",
                "opal");

        PasswordDaoAuthenticationProvider provider = new PasswordDaoAuthenticationProvider();
        provider.setPasswordAuthenticationDao(new MockAuthenticationDaoUserPeterCredentialsExpired());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown CredentialsExpiredException");
        } catch (CredentialsExpiredException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticateFailsIfUserDisabled() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("peter",
                "opal");

        PasswordDaoAuthenticationProvider provider = new PasswordDaoAuthenticationProvider();
        provider.setPasswordAuthenticationDao(new MockAuthenticationDaoUserPeter());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown DisabledException");
        } catch (DisabledException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticateFailsWhenAuthenticationDaoHasBackendFailure() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("marissa",
                "koala");

        PasswordDaoAuthenticationProvider provider = new PasswordDaoAuthenticationProvider();
        provider.setPasswordAuthenticationDao(new MockAuthenticationDaoSimulateBackendError());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown AuthenticationServiceException");
        } catch (AuthenticationServiceException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticateFailsWithInvalidPassword() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("marissa",
                "INVALID_PASSWORD");

        PasswordDaoAuthenticationProvider provider = new PasswordDaoAuthenticationProvider();
        provider.setPasswordAuthenticationDao(new MockAuthenticationDaoUserMarissa());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown BadCredentialsException");
        } catch (BadCredentialsException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticateFailsWithInvalidUsername() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("INVALID_USER",
                "koala");

        PasswordDaoAuthenticationProvider provider = new PasswordDaoAuthenticationProvider();
        provider.setPasswordAuthenticationDao(new MockAuthenticationDaoUserMarissa());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown BadCredentialsException");
        } catch (BadCredentialsException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticateFailsWithMixedCaseUsernameIfDefaultChanged() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("MaRiSSA",
                "koala");

        PasswordDaoAuthenticationProvider provider = new PasswordDaoAuthenticationProvider();
        provider.setPasswordAuthenticationDao(new MockAuthenticationDaoUserMarissa());
        provider.setUserCache(new MockUserCache());

        try {
            provider.authenticate(token);
            fail("Should have thrown BadCredentialsException");
        } catch (BadCredentialsException expected) {
            assertTrue(true);
        }
    }

    public void testAuthenticates() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("marissa",
                "koala");
        token.setDetails("192.168.0.1");

        PasswordDaoAuthenticationProvider provider = new PasswordDaoAuthenticationProvider();
        provider.setPasswordAuthenticationDao(new MockAuthenticationDaoUserMarissa());
        provider.setUserCache(new MockUserCache());

        Authentication result = provider.authenticate(token);

        if (!(result instanceof UsernamePasswordAuthenticationToken)) {
            fail(
                "Should have returned instance of UsernamePasswordAuthenticationToken");
        }

        UsernamePasswordAuthenticationToken castResult = (UsernamePasswordAuthenticationToken) result;
        assertEquals(User.class, castResult.getPrincipal().getClass());
        assertEquals("koala", castResult.getCredentials());
        assertEquals("ROLE_ONE", castResult.getAuthorities()[0].getAuthority());
        assertEquals("ROLE_TWO", castResult.getAuthorities()[1].getAuthority());
        assertEquals("192.168.0.1", castResult.getDetails());
    }

    public void testAuthenticatesASecondTime() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("marissa",
                "koala");

        PasswordDaoAuthenticationProvider provider = new PasswordDaoAuthenticationProvider();
        provider.setPasswordAuthenticationDao(new MockAuthenticationDaoUserMarissa());
        provider.setUserCache(new MockUserCache());

        Authentication result = provider.authenticate(token);

        if (!(result instanceof UsernamePasswordAuthenticationToken)) {
            fail(
                "Should have returned instance of UsernamePasswordAuthenticationToken");
        }

        // Now try to authenticate with the previous result (with its UserDetails)
        Authentication result2 = provider.authenticate(result);

        if (!(result2 instanceof UsernamePasswordAuthenticationToken)) {
            fail(
                "Should have returned instance of UsernamePasswordAuthenticationToken");
        }

        assertEquals(result.getCredentials(), result2.getCredentials());
    }

    public void testAuthenticatesWithForcePrincipalAsString() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("marissa",
                "koala");

        PasswordDaoAuthenticationProvider provider = new PasswordDaoAuthenticationProvider();
        provider.setPasswordAuthenticationDao(new MockAuthenticationDaoUserMarissa());
        provider.setUserCache(new MockUserCache());
        provider.setForcePrincipalAsString(true);

        Authentication result = provider.authenticate(token);

        if (!(result instanceof UsernamePasswordAuthenticationToken)) {
            fail(
                "Should have returned instance of UsernamePasswordAuthenticationToken");
        }

        UsernamePasswordAuthenticationToken castResult = (UsernamePasswordAuthenticationToken) result;
        assertEquals(String.class, castResult.getPrincipal().getClass());
        assertEquals("marissa", castResult.getPrincipal());
    }

    public void testGettersSetters() {
        PasswordDaoAuthenticationProvider provider = new PasswordDaoAuthenticationProvider();
        provider.setUserCache(new EhCacheBasedUserCache());
        assertEquals(EhCacheBasedUserCache.class,
            provider.getUserCache().getClass());

        assertFalse(provider.isForcePrincipalAsString());
        provider.setForcePrincipalAsString(true);
        assertTrue(provider.isForcePrincipalAsString());
    }

    public void testStartupFailsIfNoAuthenticationDao()
        throws Exception {
        PasswordDaoAuthenticationProvider provider = new PasswordDaoAuthenticationProvider();

        try {
            provider.afterPropertiesSet();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }
    }

    public void testStartupFailsIfNoUserCacheSet() throws Exception {
        PasswordDaoAuthenticationProvider provider = new PasswordDaoAuthenticationProvider();
        provider.setPasswordAuthenticationDao(new MockAuthenticationDaoUserMarissa());
        assertEquals(NullUserCache.class, provider.getUserCache().getClass());
        provider.setUserCache(null);

        try {
            provider.afterPropertiesSet();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }
    }

    public void testStartupSuccess() throws Exception {
        PasswordDaoAuthenticationProvider provider = new PasswordDaoAuthenticationProvider();
        PasswordAuthenticationDao dao = new MockAuthenticationDaoUserMarissa();
        provider.setPasswordAuthenticationDao(dao);
        provider.setUserCache(new MockUserCache());
        assertEquals(dao, provider.getPasswordAuthenticationDao());
        provider.afterPropertiesSet();
        assertTrue(true);
    }

    public void testSupports() {
        PasswordDaoAuthenticationProvider provider = new PasswordDaoAuthenticationProvider();
        assertTrue(provider.supports(UsernamePasswordAuthenticationToken.class));
        assertTrue(!provider.supports(TestingAuthenticationToken.class));
    }

    //~ Inner Classes ==========================================================

    private class MockAuthenticationDaoSimulateBackendError
        implements PasswordAuthenticationDao {
        public UserDetails loadUserByUsernameAndPassword(String username,
            String password)
            throws BadCredentialsException, DataAccessException {
            throw new DataRetrievalFailureException(
                "This mock simulator is designed to fail");
        }
    }

    private class MockAuthenticationDaoUserMarissa
        implements PasswordAuthenticationDao {
        public UserDetails loadUserByUsernameAndPassword(String username,
            String password)
            throws BadCredentialsException, DataAccessException {
            if ("marissa".equals(username) && "koala".equals(password)) {
                return new User("marissa", "koala", true, true, true, true,
                    new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_ONE"), new GrantedAuthorityImpl(
                            "ROLE_TWO")});
            } else {
                throw new BadCredentialsException("Invalid credentials");
            }
        }
    }

    private class MockAuthenticationDaoUserPeter
        implements PasswordAuthenticationDao {
        public UserDetails loadUserByUsernameAndPassword(String username,
            String password)
            throws BadCredentialsException, DataAccessException {
            if ("peter".equals(username) && "opal".equals(password)) {
                return new User("peter", "opal", false, true, true, true,
                    new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_ONE"), new GrantedAuthorityImpl(
                            "ROLE_TWO")});
            } else {
                throw new BadCredentialsException("Invalid credentials");
            }
        }
    }

    private class MockAuthenticationDaoUserPeterAccountExpired
        implements PasswordAuthenticationDao {
        public UserDetails loadUserByUsernameAndPassword(String username,
            String password)
            throws UsernameNotFoundException, DataAccessException {
            if ("peter".equals(username)) {
                return new User("peter", "opal", true, false, true, true,
                    new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_ONE"), new GrantedAuthorityImpl(
                            "ROLE_TWO")});
            } else {
                throw new UsernameNotFoundException("Could not find: "
                    + username);
            }
        }
    }

    private class MockAuthenticationDaoUserPeterAccountLocked
        implements PasswordAuthenticationDao {
        public UserDetails loadUserByUsernameAndPassword(String username,
            String password)
            throws UsernameNotFoundException, DataAccessException {
            if ("peter".equals(username)) {
                return new User("peter", "opal", true, true, true, false,
                    new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_ONE"), new GrantedAuthorityImpl(
                            "ROLE_TWO")});
            } else {
                throw new UsernameNotFoundException("Could not find: "
                    + username);
            }
        }
    }

    private class MockAuthenticationDaoUserPeterCredentialsExpired
        implements PasswordAuthenticationDao {
        public UserDetails loadUserByUsernameAndPassword(String username,
            String password)
            throws UsernameNotFoundException, DataAccessException {
            if ("peter".equals(username)) {
                return new User("peter", "opal", true, true, false, true,
                    new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_ONE"), new GrantedAuthorityImpl(
                            "ROLE_TWO")});
            } else {
                throw new UsernameNotFoundException("Could not find: "
                    + username);
            }
        }
    }

    private class MockUserCache implements UserCache {
        private Map cache = new HashMap();

        public UserDetails getUserFromCache(String username) {
            return (User) cache.get(username);
        }

        public void putUserInCache(UserDetails user) {
            cache.put(user.getUsername(), user);
        }

        public void removeUserFromCache(String username) {}
    }
}
