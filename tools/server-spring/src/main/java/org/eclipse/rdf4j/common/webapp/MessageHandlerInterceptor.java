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
package org.eclipse.rdf4j.common.webapp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Interceptor that inserts some commonly used values into the model. The inserted values are: - path, equal to
 * request.getContextPath() (e.g. /context) - basePath, equal to the fully qualified context path (e.g.
 * http://www.example.com/context/) - currentYear, equal to the current year
 *
 * @author Herko ter Horst
 */
public class MessageHandlerInterceptor implements HandlerInterceptor {

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) {
		// nop
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView mav) {
		HttpSession session = request.getSession();

		if (session != null) {
			Message message = (Message) session.getAttribute(Message.ATTRIBUTE_KEY);
			if (message != null && !mav.getModelMap().containsKey(Message.ATTRIBUTE_KEY)) {
				mav.addObject(Message.ATTRIBUTE_KEY, message);
			}

			boolean shouldRemove = true;
			if (mav.hasView() && mav.getView() instanceof RedirectView) {
				shouldRemove = false;
			}
			if (mav.getViewName() != null && mav.getViewName().startsWith("redirect:")) {
				shouldRemove = false;
			}

			if (shouldRemove) {
				session.removeAttribute(Message.ATTRIBUTE_KEY);
			}
		}
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		return true;
	}

}
