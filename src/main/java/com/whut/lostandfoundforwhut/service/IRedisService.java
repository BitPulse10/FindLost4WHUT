package com.whut.lostandfoundforwhut.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author DXR
 * @date 2026/01/30
 * @description Redis 服务接口，定义常用缓存操作
 */
public interface IRedisService {

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 设置指定 key 的值
     * @param key 键
     * @param value 值
     */
    void setValue(String key, Object value);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 设置指定 key 的值并设置过期时间
     * @param key 键
     * @param value 值
     * @param ttl 过期时间
     * Duration.ofHours(2)（2 小时）、Duration.ofMinutes(30)（30 分钟）
     */
    void setValue(String key, Object value, Duration ttl);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 设置指定 key 的值并设置过期时间（毫秒）
     * @param key 键
     * @param value 值
     * @param expiredMs 过期时间（毫秒）
     * 7200000（2 小时）、1800000（30 分钟）
     */
    void setValue(String key, Object value, long expiredMs);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 获取指定 key 的值
     * @param key 键
     * @return 值
     */
    Object getValue(String key);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 删除指定 key
     * @param key 键
     * @return 是否删除成功
     */
    Boolean remove(String key);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 判断 key 是否存在
     * @param key 键
     * @return 是否存在
     */
    Boolean isExists(String key);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 设置 key 的过期时间
     * @param key 键
     * @param ttl 过期时间
     * @return 是否设置成功
     */
    Boolean expire(String key, Duration ttl);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 自增 key
     * @param key 键
     * @return 自增后的值
     */
    Long increment(String key);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 指定增量自增 key
     * @param key 键
     * @param delta 增量
     * @return 自增后的值
     */
    Long incrementBy(String key, long delta);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 自减 key
     * @param key 键
     * @return 自减后的值
     */
    Long decrement(String key);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 指定增量自减 key
     * @param key 键
     * @param delta 增量
     * @return 自减后的值
     */
    Long decrementBy(String key, long delta);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 写入 Hash 字段
     * @param key 键
     * @param field 字段
     * @param value 值
     */
    void putToMap(String key, String field, Object value);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 获取 Hash 字段值
     * @param key 键
     * @param field 字段
     * @return 字段值
     */
    Object getFromMap(String key, String field);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 获取 Hash 全量
     * @param key 键
     * @return Hash 集合
     */
    Map<Object, Object> getAllFromMap(String key);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 批量写入 Hash
     * @param key 键
     * @param map 字段集合
     */
    void putAllToMap(String key, Map<String, Object> map);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 删除 Hash 字段
     * @param key 键
     * @param fields 字段集合
     * @return 删除数量
     */
    Long removeFromMap(String key, String... fields);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 添加到列表尾部
     * @param key 键
     * @param value 值
     * @return 列表长度
     */
    Long addToList(String key, Object value);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 从列表获取指定索引
     * @param key 键
     * @param index 索引
     * @return 元素
     */
    Object getFromList(String key, long index);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 获取列表区间
     * @param key 键
     * @param start 起始
     * @param end 结束
     * @return 元素列表
     */
    List<Object> rangeFromList(String key, long start, long end);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 添加到集合
     * @param key 键
     * @param values 值集合
     * @return 添加数量
     */
    Long addToSet(String key, Object... values);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 判断是否是集合成员
     * @param key 键
     * @param value 值
     * @return 是否存在
     */
    Boolean isSetMember(String key, Object value);

    /**
     * @author DXR
     * @date 2026/01/30
     * @description 获取集合全部成员
     * @param key 键
     * @return 成员集合
     */
    Set<Object> getSetMembers(String key);

    /**
     * @author Codex
     * @date 2026/02/09
     * @description 按前缀删除 key
     * @param prefix 前缀
     * @return 删除数量
     */
    Long removeByPrefix(String prefix);
}
