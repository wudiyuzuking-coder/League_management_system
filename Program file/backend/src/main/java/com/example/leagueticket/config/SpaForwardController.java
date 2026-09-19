package com.example.leagueticket.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Sends known Vue Router history routes to the packaged SPA entry point.
 *
 * <p>The mappings are deliberately limited to frontend route roots. API,
 * upload and asset URLs are never caught by this fallback.</p>
 */
@Controller
public class SpaForwardController {

    @GetMapping({
            "/",
            "/login",
            "/register",
            "/switch-account",
            "/403",
            "/user",
            "/user/**",
            "/club",
            "/club/**",
            "/admin",
            "/admin/**"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
