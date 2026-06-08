package com.corwin.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller

/**
 * @author Corwin 2026/3/30
 */
public class SpaForwardController {

    @RequestMapping({"/{path:^(?!api$)[^\\.]*}", "/{path:^(?!api$)[^\\.]*}/**"})
    public String forward() {
        return "forward:/index.html";
    }

}
