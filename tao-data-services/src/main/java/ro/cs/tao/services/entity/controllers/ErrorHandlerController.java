package ro.cs.tao.services.entity.controllers;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErrorHandlerController implements ErrorController {

    @GetMapping("/error")
    public String customError() {
        return "An unexpected error has occurred";
    }

    public String getErrorPath() {
        return "/error";
    }
}
