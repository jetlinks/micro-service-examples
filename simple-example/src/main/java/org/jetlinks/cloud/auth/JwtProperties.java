package org.jetlinks.cloud.auth;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.JWTVerifier;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;

@Getter
@Setter
@ConfigurationProperties(prefix = "jetlinks.token.jwt")
public class JwtProperties {

    private String key;

    private String issuer;

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private Algorithm decoder;

    @SneakyThrows
    public Algorithm getDecoderAlgorithm() {
        if (decoder != null) {
            return decoder;
        }

        byte[] keyBytes = Base64.decodeBase64(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey key = keyFactory.generatePublic(keySpec);
        return decoder = Algorithm.RSA256((RSAPublicKey) key, null);
    }

    public JWTVerifier createVerifier(){
        Algorithm algorithm = getDecoderAlgorithm();

        return JWT.require(algorithm)
                .withIssuer(getIssuer())
                .build();
    }
}
