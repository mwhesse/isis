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

package org.apache.isis.metamodel.facets.param.hide.method;

import org.apache.isis.commons.collections.Can;
import org.apache.isis.metamodel.commons.StringExtensions;
import org.apache.isis.metamodel.exceptions.MetaModelException;
import org.apache.isis.metamodel.facetapi.FeatureType;
import org.apache.isis.metamodel.facets.DependentArgUtils;
import org.apache.isis.metamodel.facets.DependentArgUtils.ParamSupportingMethodSearchRequest.ReturnType;
import org.apache.isis.metamodel.facets.MethodLiteralConstants;
import org.apache.isis.metamodel.facets.MethodPrefixBasedFacetFactoryAbstract;
import org.apache.isis.metamodel.facets.param.hide.ActionParameterHiddenFacet;

import lombok.val;

/**
 * Sets up {@link ActionParameterHiddenFacet}.
 */
public class ActionParameterHiddenFacetViaMethodFactory extends MethodPrefixBasedFacetFactoryAbstract  {

    private static final Can<String> PREFIXES = Can.ofSingleton(MethodLiteralConstants.HIDE_PREFIX);

    public ActionParameterHiddenFacetViaMethodFactory() {
        //super(FeatureType.PARAMETERS_ONLY, OrphanValidation.VALIDATE, PREFIXES);
        super(FeatureType.ACTIONS_ONLY, OrphanValidation.VALIDATE, PREFIXES);
    }

    @Override
    public void process(final ProcessMethodContext processMethodContext) {

        val facetedMethod = processMethodContext.getFacetHolder();
        val parameters = facetedMethod.getParameters();

        if (parameters.isEmpty()) {
            return;
        }

        // attach ActionParameterHiddenFacet if hideNumMethod is found ...
        
        val actionMethod = processMethodContext.getMethod();
        val capitalizedName = StringExtensions.asCapitalizedName(actionMethod.getName());

        val searchRequest = DependentArgUtils.ParamSupportingMethodSearchRequest.builder()
                .processMethodContext(processMethodContext)
                .returnType(ReturnType.BOOLEAN)
                .paramIndexToMethodName(paramIndex -> 
                    MethodLiteralConstants.HIDE_PREFIX + paramIndex + capitalizedName)
                .build();
        
        DependentArgUtils.findParamSupportingMethods(searchRequest, searchResult -> {
            
            val hideMethod = searchResult.getSupportingMethod();
            val paramIndex = searchResult.getParamIndex();
            
            processMethodContext.removeMethod(hideMethod);

            if (facetedMethod.containsNonFallbackFacet(ActionParameterHiddenFacet.class)) {
                val cls = processMethodContext.getCls();
                throw new MetaModelException(cls + " uses both old and new 'hide' syntax - "
                        + "must use one or other");
            }
            
            // add facets directly to parameters, not to actions
            val paramAsHolder = parameters.get(paramIndex);
            
            super.addFacet(
                    new ActionParameterHiddenFacetViaMethod(hideMethod, paramAsHolder));
        });
        
    }
    

    @Override
    public void processParams(final ProcessParameterContext processParameterContext) {

      //FIXME remove
        
//        final Class<?> cls = processParameterContext.getCls();
//        final Method actionMethod = processParameterContext.getMethod();
//        final int paramNum = processParameterContext.getParamNum();
//        final IdentifiedHolder facetHolder = processParameterContext.getFacetHolder();
//
//        final String capitalizedName = StringExtensions.asCapitalizedName(actionMethod.getName());
//        final List<Class<?>> paramTypes = ListExtensions.mutableCopy(actionMethod.getParameterTypes());
//
//        final String hideName = MethodLiteralConstants.HIDE_PREFIX + paramNum + capitalizedName;
//
//        final int numParamTypes = paramTypes.size();
//
//        for(int i=0; i< numParamTypes+1; i++) {
//            val hideMethod = MethodFinderUtils.findMethod(
//                    cls,
//                    hideName,
//                    boolean.class,
//                    NO_ARG);
//
//            if (hideMethod != null) {
//                processParameterContext.removeMethod(hideMethod);
//
//                final Facet facet = new ActionParameterHiddenFacetViaMethod(hideMethod, facetHolder);
//                super.addFacet(facet);
//                return;
//            }
//
//            // remove last, and search again
//            if(!paramTypes.isEmpty()) {
//                paramTypes.remove(paramTypes.size()-1);
//            }
//        }
//
    }

}
