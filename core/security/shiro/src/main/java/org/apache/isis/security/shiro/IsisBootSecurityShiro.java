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
package org.apache.isis.security.shiro;

import javax.inject.Singleton;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.apache.isis.runtime.services.auth.AuthorizationManagerStandard;
import org.apache.isis.security.api.authentication.standard.AuthenticationManagerStandard;
import org.apache.isis.security.api.authentication.standard.Authenticator;
import org.apache.isis.security.api.authorization.standard.Authorizor;
import org.apache.isis.security.shiro.authentication.ShiroAuthenticator;
import org.apache.isis.security.shiro.authorization.ShiroAuthorizor;

/**
 * Configuration Bean to support Isis Security using Shiro.
 *  
 * @since 2.0
 */
@Configuration
@Import({
    AuthorizationManagerStandard.class,
    AuthenticationManagerStandard.class,
    WebModuleShiro.class
})
public class IsisBootSecurityShiro {

    @Bean @Singleton
    public Authenticator authenticator() {
        return new ShiroAuthenticator();
    }

    @Bean @Singleton
    public Authorizor autorizor() {
        return new ShiroAuthorizor();
    }


}
