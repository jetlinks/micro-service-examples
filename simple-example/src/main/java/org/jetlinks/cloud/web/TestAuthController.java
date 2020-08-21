package org.jetlinks.cloud.web;

import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth-info")
public class TestAuthController {

    @GetMapping
    public Authentication getAuthInfoJson(){
        //获取当前用户权限信息
        return Authentication
                .current()
                .orElseThrow(UnAuthorizedException::new);
    }

}
