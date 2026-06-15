package com.mianmiantong.service.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
public class VerificationCodeService {

    private static final String CODE_PREFIX = "email:code:";
    private static final String RATE_PREFIX = "email:rate:";
    private static final int CODE_TTL_MINUTES = 5;
    private static final int RATE_TTL_SECONDS = 60;
    private static final int CODE_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redis;
    private final SecureRandom random = new SecureRandom();

    public VerificationCodeService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Generate a 6-digit code, store in Redis, return the code. */
    public String generateAndStore(String email, String type) {
        String rateKey = RATE_PREFIX + type + ":" + email;
        if (Boolean.TRUE.equals(redis.hasKey(rateKey))) {
            throw new IllegalArgumentException("验证码发送过于频繁，请60秒后再试");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        String codeKey = CODE_PREFIX + type + ":" + email;

        redis.opsForValue().set(codeKey, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        redis.opsForValue().set(rateKey, "1", RATE_TTL_SECONDS, TimeUnit.SECONDS);

        // Store attempt counter separately
        redis.opsForValue().set(codeKey + ":attempts", "0", CODE_TTL_MINUTES, TimeUnit.MINUTES);

        return code;
    }

    /** Verify the code is correct. Throws if invalid or expired. Does NOT delete the code. */
    public void verify(String email, String type, String code) {
        String codeKey = CODE_PREFIX + type + ":" + email;
        String storedCode = redis.opsForValue().get(codeKey);

        if (storedCode == null) {
            throw new IllegalArgumentException("验证码已过期，请重新获取");
        }

        String attemptKey = codeKey + ":attempts";
        String attemptsStr = redis.opsForValue().get(attemptKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if (attempts >= MAX_ATTEMPTS) {
            redis.delete(codeKey);
            redis.delete(attemptKey);
            throw new IllegalArgumentException("验证码尝试次数过多，请重新获取");
        }

        redis.opsForValue().increment(attemptKey);

        if (!storedCode.equals(code)) {
            throw new IllegalArgumentException("验证码错误");
        }
    }

    /** Delete the verification code from Redis after successful operation. */
    public void consumeCode(String email, String type) {
        String codeKey = CODE_PREFIX + type + ":" + email;
        String attemptKey = codeKey + ":attempts";
        redis.delete(codeKey);
        redis.delete(attemptKey);
    }
}
