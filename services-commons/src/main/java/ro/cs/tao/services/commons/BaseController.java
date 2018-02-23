package ro.cs.tao.services.commons;

import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * @author Cosmin Cara
 */
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:63343"})
//@RequestMapping("/tao")
public class BaseController {

/*    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session) {
        session.invalidate();
    }*/
}
