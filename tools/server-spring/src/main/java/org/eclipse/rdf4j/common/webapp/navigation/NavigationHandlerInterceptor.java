/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.common.webapp.navigation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interceptor that inserts the navigation model for the current Spring view into the model.
 *
 * @author Herko ter Horst
 */
public class NavigationHandlerInterceptor implements HandlerInterceptor {

	private NavigationModel navigationModel;

	public NavigationModel getNavigationModel() {
		return navigationModel;
	}

	public void setNavigationModel(NavigationModel navigationModel) {
		this.navigationModel = navigationModel;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) {
		// nop
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView mav) {
		NavigationModel sessionNavigationModel = (NavigationModel) request.getSession()
				.getAttribute(NavigationModel.NAVIGATION_MODEL_KEY);
		if (sessionNavigationModel == null) {
			sessionNavigationModel = navigationModel;
		}

		if (mav != null && sessionNavigationModel != null) {
			mav.addObject("view", sessionNavigationModel
					.findView(request.getRequestURI().substring(request.getContextPath().length())));
		}
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		HttpSession session = request.getSession(true);
		if (session.getAttribute(NavigationModel.NAVIGATION_MODEL_KEY) == null) {
			session.setAttribute(NavigationModel.NAVIGATION_MODEL_KEY, getNavigationModel().clone());
		}

		return true;
	}

}
