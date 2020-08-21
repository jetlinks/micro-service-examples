package org.jetlinks.cloud.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.AuthenticationHolder;
import org.hswebframework.web.authorization.AuthenticationSupplier;
import org.hswebframework.web.authorization.builder.AuthenticationBuilderFactory;
import org.hswebframework.web.authorization.simple.builder.SimpleAuthenticationBuilderFactory;
import org.hswebframework.web.authorization.simple.builder.SimpleDataAccessConfigBuilderFactory;
import org.hswebframework.web.context.ContextUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

public class JwtAuthSupplier extends HandlerInterceptorAdapter implements AuthenticationSupplier {

    private final JwtProperties jwt;

    public JwtAuthSupplier(JwtProperties jwtProperties) {
        this.jwt = jwtProperties;
        AuthenticationHolder.addSupplier(this);
    }

    final AuthenticationBuilderFactory factory = new SimpleAuthenticationBuilderFactory(new SimpleDataAccessConfigBuilderFactory());

    //todo 使用feginClient获取指定用户的权限信息
    public Optional<Authentication> get(String userId) {
        return Optional.empty();
    }

    public Optional<Authentication> get() {
        return ContextUtils.currentContext().get(Authentication.class);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String token = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isEmpty(token) || !token.startsWith("jwt")) {
            return true;
        }
        JWTVerifier verifier = jwt.createVerifier();
        DecodedJWT jwt = verifier.verify(token.substring(4));

        ContextUtils.currentContext().put(Authentication.class, factory.create().json(jwt.getSubject()).build());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //清空上下文防止内存泄漏
        ContextUtils.currentContext().clean();
    }
}
