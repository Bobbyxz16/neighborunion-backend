package com.example.neighborhelp.security;

import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.repository.UserRepository; // Asume que tienes esto (JPA repo)
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component  // Spring lo detecta automáticamente
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;  // Para cargar UserDetails desde DB
    private final UserRepository userRepository;  // Opcional: Si necesitas cargar User por ID

    // Constructor injection
    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extrae el token del header Authorization
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No hay token: Continúa con el chain (para endpoints públicos)
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);  // Quita "Bearer "
        userEmail = jwtService.extractUsername(jwt);  // Usa tu método

        // 2. Si ya hay autenticación en el contexto (e.g., de login), salta
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 3. Valida el token usando tu JwtService
            if (jwtService.validateToken(jwt)) {
                // 4. Carga UserDetails desde DB (usa email como username)
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                // Opcional: Verifica que el userId en token coincida con DB (extra seguridad)
                Long tokenUserId = jwtService.extractUserId(jwt);
                User userFromDb = userRepository.findById(tokenUserId).orElse(null);
                if (userFromDb != null && !userFromDb.getEmail().equals(userEmail)) {
                    // Token inválido (userId no coincide)
                    filterChain.doFilter(request, response);
                    return;
                }

                // 5. Crea Authentication con authorities (roles)
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                // 6. Set details de la request (IP, session, etc.)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. Guarda en SecurityContext (ahora el usuario está "autenticado")
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 8. Continúa con el chain (llega al controlador)
        filterChain.doFilter(request, response);
    }
}
