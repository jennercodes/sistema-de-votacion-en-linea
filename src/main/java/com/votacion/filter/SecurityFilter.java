package com.votacion.filter;

import com.votacion.bean.LoginBean;
import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(filterName = "SecurityFilter", urlPatterns = {"/faces/admin/*", "/admin/*"})
public class SecurityFilter implements Filter {

    @Inject
    private LoginBean loginBean;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Obtener el bean de la sesión como fallback si la inyección falla
        LoginBean bean = this.loginBean;
        if (bean == null) {
            bean = (LoginBean) httpRequest.getSession().getAttribute("loginBean");
        }

        // Si no está logueado o no es administrador, redirigir al login
        if (bean == null || !bean.isAdmin()) {
            String contextPath = httpRequest.getContextPath();
            httpResponse.sendRedirect(contextPath + "/login.xhtml");
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {}
}
