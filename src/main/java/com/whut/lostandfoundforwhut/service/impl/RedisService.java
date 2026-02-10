package com.whut.lostandfoundforwhut.service.impl;

import com.whut.lostandfoundforwhut.service.IRedisService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author DXR
 * @date 2026/01/30
 * @description Redis 服务实现，封装常用操作
 */
@Service
public class RedisService implements IRedisService {
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 构造函数注入 RedisTemplate
     * @param redisTemplate RedisTemplate
     */
    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void setValue(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void setValue(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public void setValue(String key, Object value, long expiredMs) {
        redisTemplate.opsForValue().set(key, value, Duration.ofMillis(expiredMs));
    }

    @Override
    public Object getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public Boolean remove(String key) {
        return redisTemplate.delete(key);
    }

    @Override
    public Boolean isExists(String key) {
        return redisTemplate.hasKey(key);
    }

    @Override
    public Boolean expire(String key, Duration ttl) {
        return redisTemplate.expire(key, ttl);
    }

    @Override
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    @Override
    public Long incrementBy(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    @Override
    public Long decrement(String key) {
        return redisTemplate.opsForValue().increment(key, -1);
    }

    @Override
    public Long decrementBy(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, -delta);
    }

    @Override
    public void putToMap(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    @Override
    public Object getFromMap(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    @Override
    public Map<Object, Object> getAllFromMap(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    @Override
    public void putAllToMap(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    @Override
    public Long removeFromMap(String key, String... fields) {
        return redisTemplate.opsForHash().delete(key, (Object[]) fields);
    }

    @Override
    public Long addToList(String key, Object value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    @Override
    public Object getFromList(String key, long index) {
        return redisTemplate.opsForList().index(key, index);
    }

    @Override
    public List<Object> rangeFromList(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    @Override
    public Long addToSet(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    @Override
    public Boolean isSetMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    @Override
    public Set<Object> getSetMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    @Override
    public Long removeByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return 0L;
        }
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        return redisTemplate.delete(keys);
    }
}
