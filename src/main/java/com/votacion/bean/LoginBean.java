package com.votacion.bean;

import com.votacion.dao.UsuarioDAO;
import com.votacion.model.Usuario;
import org.mindrot.jbcrypt.BCrypt;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@SessionScoped
public class LoginBean implements Serializable {

    private String username;
    private String password;
    private Usuario usuario;
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public String login() {
        Usuario user = usuarioDAO.buscarPorUsername(username);
        if (user != null && BCrypt.checkpw(password, user.getPasswordHash())) {
            this.usuario = user;
            // Redirigir según el rol del usuario
            if ("ADMIN".equals(user.getRol())) {
                return "/admin/encuestas?faces-redirect=true";
            } else {
                return "/index?faces-redirect=true";
            }
        }
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error de autenticación", "Usuario o contraseña incorrectos"));
        return null;
    }

    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/login?faces-redirect=true";
    }

    public boolean isLoggedIn() {
        return usuario != null;
    }

    public boolean isAdmin() {
        return usuario != null && "ADMIN".equals(usuario.getRol());
    }

    public boolean isVotante() {
        return usuario != null && "VOTANTE".equals(usuario.getRol());
    }

    // Getters y Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
