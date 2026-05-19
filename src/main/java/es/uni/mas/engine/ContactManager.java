package es.uni.mas.engine;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ContactManager {
    
    private Set<String> whitelist;
    private Set<String> blacklist;
    private Path configPath;
    
    private static final String CONFIG_FILE = "contacts-config.json";
    
    // Singleton instance
    private static ContactManager instance;
    
    // Contactos conocidos en el sistema
    private static final Set<String> KNOWN_CONTACTS = Set.of(
            "Profesor_Avisos",
            "Grupo_Trabajo_IA",
            "Compañero_Clase",
            "AmigoCercano",
            "GrupoUni",
            "GrupoFamilia",
            "Desconocido",
            "Notificaciones_App"
    );
    
    // Private constructor para singleton
    private ContactManager() {
        this.whitelist = new HashSet<>();
        this.blacklist = new HashSet<>();
        this.configPath = Paths.get(CONFIG_FILE);
        loadConfig();
    }
    
    /**
     * Obtiene la instancia única de ContactManager
     */
    public static synchronized ContactManager getInstance() {
        if (instance == null) {
            instance = new ContactManager();
        }
        return instance;
    }
    
    /**
     * Determina si un contacto está permitido
     * @return ALLOW si está en whitelist, BLOCK si está en blacklist, DEFAULT si no está configurado
     */
    public ContactStatus checkContact(String sender) {
        if (whitelist.contains(sender)) {
            return ContactStatus.ALLOW;
        }
        if (blacklist.contains(sender)) {
            return ContactStatus.BLOCK;
        }
        return ContactStatus.DEFAULT;
    }
    
    public void allowContact(String sender) {
        blacklist.remove(sender);
        whitelist.add(sender);
        saveConfig();
    }
    
    public void blockContact(String sender) {
        whitelist.remove(sender);
        blacklist.add(sender);
        saveConfig();
    }
    
    public void resetContact(String sender) {
        whitelist.remove(sender);
        blacklist.remove(sender);
        saveConfig();
    }
    
    public Set<String> getWhitelist() {
        return new HashSet<>(whitelist);
    }
    
    public Set<String> getBlacklist() {
        return new HashSet<>(blacklist);
    }
    
    /**
     * Devuelve todos los contactos conocidos en el sistema
     */
    public Set<String> getAllKnownContacts() {
        return new HashSet<>(KNOWN_CONTACTS);
    }
    
    private void loadConfig() {
        try {
            if (Files.exists(configPath)) {
                String content = Files.readString(configPath);
                JSONObject json = new JSONObject(content);
                
                JSONArray wl = json.optJSONArray("whitelist");
                if (wl != null) {
                    for (int i = 0; i < wl.length(); i++) {
                        whitelist.add(wl.getString(i));
                    }
                }
                
                JSONArray bl = json.optJSONArray("blacklist");
                if (bl != null) {
                    for (int i = 0; i < bl.length(); i++) {
                        blacklist.add(bl.getString(i));
                    }
                }
                
                System.out.println("ContactManager: cargada configuración. " +
                        "Whitelist: " + whitelist.size() + ", Blacklist: " + blacklist.size());
            } else {
                System.out.println("ContactManager: archivo de configuración no encontrado. Usando vacío.");
            }
        } catch (IOException e) {
            System.err.println("Error cargando configuración: " + e.getMessage());
        }
    }
    
    private void saveConfig() {
        try {
            JSONObject json = new JSONObject();
            json.put("whitelist", new JSONArray(new ArrayList<>(whitelist)));
            json.put("blacklist", new JSONArray(new ArrayList<>(blacklist)));
            
            Files.writeString(configPath, json.toString(2));
            System.out.println("ContactManager: configuración guardada.");
        } catch (IOException e) {
            System.err.println("Error guardando configuración: " + e.getMessage());
        }
    }
    
    public enum ContactStatus {
        ALLOW,      // En whitelist, permitir siempre
        BLOCK,      // En blacklist, bloquear siempre
        DEFAULT     // Por defecto, aplicar reglas
    }
}
