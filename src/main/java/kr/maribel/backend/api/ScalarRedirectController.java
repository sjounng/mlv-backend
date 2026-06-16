package kr.maribel.backend.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ScalarRedirectController {

    @GetMapping("/scalar/")
    String scalarTrailingSlash() {
        return "redirect:/scalar";
    }
}
