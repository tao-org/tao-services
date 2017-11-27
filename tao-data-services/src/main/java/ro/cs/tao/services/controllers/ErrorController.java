package ro.cs.tao.services.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Cosmin Cara
 */
@RestController
public class ErrorController implements org.springframework.boot.autoconfigure.web.ErrorController {
    private static final String errorPath = "/error";

    @RequestMapping(value = errorPath)
    public String error() {
        return "Method not supported";
    }

    @Override
    public String getErrorPath() {
        return errorPath;
    }
}
