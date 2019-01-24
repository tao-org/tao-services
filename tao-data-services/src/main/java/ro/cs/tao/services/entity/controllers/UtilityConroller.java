package ro.cs.tao.services.entity.controllers;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ro.cs.tao.datasource.util.NetStreamResponse;
import ro.cs.tao.datasource.util.NetUtils;
import ro.cs.tao.utils.StringUtilities;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

@Controller
@RequestMapping("/utilities")
public class UtilityConroller {

    @RequestMapping(value = "/tunnel/get", method = RequestMethod.GET)
    public ResponseEntity<?> get(@RequestParam("url") String url,
                                 @RequestParam("user") Optional<String> user,
                                 @RequestParam("password") Optional<String> password) {
        ResponseEntity<?> serviceResponse;
        Credentials credentials = null;
        String userName = null, pwd = null;
        if (user.isPresent()) {
            userName = user.get();
        }
        if (password.isPresent()) {
            pwd = password.get();
        }
        if (!StringUtilities.isNullOrEmpty(userName) && !StringUtilities.isNullOrEmpty(pwd)) {
            credentials = new UsernamePasswordCredentials(userName, pwd);
        }
        try {
            NetStreamResponse response = NetUtils.getResponseAsStream(url, credentials);
            serviceResponse = ResponseEntity.ok()
                    .contentLength(response.getLength())
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(Base64.getEncoder().encodeToString(response.getStream()));
        } catch (IOException e) {
            serviceResponse = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return serviceResponse;
    }
}
