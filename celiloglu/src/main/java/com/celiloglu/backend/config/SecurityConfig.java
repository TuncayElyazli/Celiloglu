package com.celiloglu.backend.config;

import com.celiloglu.backend.security.CustomUserDetailsService;
import com.celiloglu.backend.security.RoleBasedAuthenticationSuccessHandler;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final RoleBasedAuthenticationSuccessHandler authenticationSuccessHandler;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          PasswordEncoder passwordEncoder,
                          RoleBasedAuthenticationSuccessHandler authenticationSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public static HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setRequestMatcher(request -> {
            String uri = request.getRequestURI();
            return !uri.startsWith("/error") && !DispatcherType.ERROR.equals(request.getDispatcherType());
        });

        http
            .authenticationProvider(authenticationProvider())
            .sessionManagement(session -> session
                .sessionConcurrency(concurrency -> concurrency
                    .maximumSessions(-1)
                    .sessionRegistry(sessionRegistry())
                )
            )
            .requestCache(cache -> cache.requestCache(requestCache))
            .authorizeHttpRequests(authorize -> authorize
                // ERROR və ASYNC dispatcher-lər və /error marşrutu hamı üçün açıqdır
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()
                .requestMatchers("/error", "/error/**").permitAll()

                // 1. Ödəniş qeydiyyatı (POST): ADMIN, SUPER_ADMIN, CASHIER
                .requestMatchers(HttpMethod.POST, "/admin/orders/*/payments").hasAnyRole("ADMIN", "SUPER_ADMIN", "CASHIER")

                // 2. Sifariş statusunun yenilənməsi (POST): ADMIN, SUPER_ADMIN, WAREHOUSE_MANAGER
                .requestMatchers(HttpMethod.POST, "/admin/orders/*/status").hasAnyRole("ADMIN", "SUPER_ADMIN", "WAREHOUSE_MANAGER")

                // 3. Stok düzəlişi (POST): ADMIN, SUPER_ADMIN, WAREHOUSE_MANAGER
                .requestMatchers(HttpMethod.POST, "/admin/products/*/stock").hasAnyRole("ADMIN", "SUPER_ADMIN", "WAREHOUSE_MANAGER")

                // 4. Sifarişlər siyahısı və detalları (GET): ADMIN, SUPER_ADMIN, CASHIER, WAREHOUSE_MANAGER
                .requestMatchers(HttpMethod.GET, "/admin/orders", "/admin/orders.html", "/admin/orders/*", "/admin/orders/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "CASHIER", "WAREHOUSE_MANAGER")

                // 5. Kassa və ödənişlər jurnalı: ADMIN, SUPER_ADMIN, CASHIER
                .requestMatchers("/admin/payments", "/admin/payments.html", "/admin/payments/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "CASHIER")

                // 6. Müştəri Hesablaşmaları: ADMIN, SUPER_ADMIN, CASHIER
                .requestMatchers("/admin/analytics", "/admin/analytics.html", "/admin/analytics/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "CASHIER")

                // 7. Komanda və İcazələr (GET və bütün POST/yazma marşrutları): yalnız SUPER_ADMIN
                .requestMatchers("/admin/team", "/admin/team.html", "/admin/team/**").hasRole("SUPER_ADMIN")

                // 8. Qalan bütün admin marşrutları (Dashboard, İstifadəçilər, Məhsullar, Kateqoriyalar, Markalar): yalnız ADMIN və SUPER_ADMIN
                .requestMatchers("/admin", "/admin.html", "/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                // 8. Autentifikasiya olunmuş müştəri marşrutları
                .requestMatchers("/profile", "/profile.html",
                                 "/cart", "/cart.html",
                                 "/orders", "/orders.html", "/orders/**")
                    .authenticated()

                // 9. Statik resurslar
                .requestMatchers("/css/**", "/js/**", "/images/**", "/product-images/**", "/favicon.ico")
                    .permitAll()

                // 10. İctimai GET və HEAD marşrutları
                .requestMatchers(HttpMethod.GET, "/login", "/login.html")
                    .permitAll()
                .requestMatchers(HttpMethod.HEAD, "/login", "/login.html")
                    .permitAll()

                .requestMatchers(HttpMethod.GET,
                                 "/", "/index", "/index.html",
                                 "/products/**",
                                 "/about", "/about.html",
                                 "/faq", "/faq.html",
                                 "/terms", "/terms.html",
                                 "/privacy", "/privacy.html",
                                 "/cooperation", "/cooperation.html")
                    .permitAll()
                .requestMatchers(HttpMethod.HEAD,
                                 "/", "/index", "/index.html",
                                 "/products/**",
                                 "/about", "/about.html",
                                 "/faq", "/faq.html",
                                 "/terms", "/terms.html",
                                 "/privacy", "/privacy.html",
                                 "/cooperation", "/cooperation.html")
                    .permitAll()

                // Qalan bütün marşrutlar autentifikasiya tələb edir
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(Customizer.withDefaults());

        return http.build();
    }
}
