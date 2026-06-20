package com.groovycoder.dvsba;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.UUID;

@Component
public class CsrfFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
                
                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse res = (HttpServletResponse) response;
                HttpSession session = req.getSession(true);

                String path = req.getRequestURI();
                if (path.startsWith("/auth/")) {
                    chain.doFilter(request, response);
                    return;
                }
                
                String token = (String) session.getAttribute("csrfToken");
                if (token == null) {
                    token = UUID.randomUUID().toString();
                    session.setAttribute("csrfToken", token);
                }

                if ("POST".equalsIgnoreCase(req.getMethod())) {
                    String submitted = req.getParameter("_csrf");
                    if (submitted == null || !submitted.equals(token)) {
                        res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                        return;
                    }
                }
                chain.doFilter(request, response);
            }

    @Override public void init(FilterConfig filterConfig) {
        // No initialization required for this filter
    }
    @Override public void destroy() { 
        // No resources to release
    }
}