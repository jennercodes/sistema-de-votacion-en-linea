package com.votacion.bean;

import com.votacion.dao.UsuarioDAO;
import com.votacion.model.Usuario;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

@Named
@RequestScoped
public class RegistroBean {

    private String username;
    private String email;
    private String password;
    private String confirmPassword;
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public String registrar() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (username == null || username.isBlank() ||
            email == null || email.isBlank() ||
            password == null || password.isBlank()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Todos los campos son obligatorios."));
            return null;
        }

        if (!password.equals(confirmPassword)) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Las contraseñas no coinciden."));
            return null;
        }

        if (usuarioDAO.existeUsername(username)) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "El nombre de usuario ya está registrado."));
            return null;
        }

        if (usuarioDAO.existeEmail(email)) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "El correo electrónico ya está registrado."));
            return null;
        }

        Usuario nuevo = new Usuario();
        nuevo.setUsername(username);
        nuevo.setEmail(email);
        nuevo.setPasswordHash(password); // Se hashea con BCrypt dentro del DAO
        nuevo.setRol("VOTANTE");

        usuarioDAO.registrar(nuevo);

        context.getExternalContext().getFlash().setKeepMessages(true);
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", "Usuario registrado correctamente. Ahora puedes iniciar sesión."));
        return "login?faces-redirect=true";
    }

    // Getters y Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
