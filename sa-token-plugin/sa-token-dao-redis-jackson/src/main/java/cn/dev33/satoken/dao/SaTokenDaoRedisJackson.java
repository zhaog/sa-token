package cn.dev33.satoken.dao;

import cn.dev33.satoken.util.SaFoxUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Sa-Token持久层接口 [Redis版] (使用 jackson 序列化方式)
 * 
 * @author kong
 * 
 */
@Component
public class SaTokenDaoRedisJackson implements SaTokenDao {

	public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
	public static final String DATE_PATTERN = "yyyy-MM-dd";
	public static final String TIME_PATTERN = "HH:mm:ss";
	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
	public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);

	/**
	 * ObjectMapper对象 (以public作用域暴露出此对象，方便开发者二次更改配置)
	 */
	public ObjectMapper objectMapper;
	
	/**
	 * String专用
	 */
	public StringRedisTemplate stringRedisTemplate;	

	/**
	 * Object专用 
	 */
	public RedisTemplate<String, Object> objectRedisTemplate;
	
	/**
	 * 标记：是否已初始化成功
	 */
	public boolean isInit;
	
	@Autowired
	public void init(RedisConnectionFactory connectionFactory) {
		
		// 指定相应的序列化方案 
		StringRedisSerializer keySerializer = new StringRedisSerializer();
		GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();
		// 通过反射获取Mapper对象, 增加一些配置, 增强兼容性 
		try {
			Field field = GenericJackson2JsonRedisSerializer.class.getDeclaredField("mapper");
			field.setAccessible(true);
			ObjectMapper objectMapper = (ObjectMapper) field.get(valueSerializer);
			this.objectMapper = objectMapper;
			// 配置[忽略未知字段]
			this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			// 配置[时间类型转换]
			JavaTimeModule timeModule = new JavaTimeModule();
			// LocalDateTime序列化与反序列化
			timeModule.addSerializer(new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
			timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
			// LocalDate序列化与反序列化
			timeModule.addSerializer(new LocalDateSerializer(DATE_FORMATTER));
			timeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE_FORMATTER));
			// LocalTime序列化与反序列化
			timeModule.addSerializer(new LocalTimeSerializer(TIME_FORMATTER));
			timeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(TIME_FORMATTER));
			this.objectMapper.registerModule(timeModule);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		// 构建StringRedisTemplate
		StringRedisTemplate stringTemplate = new StringRedisTemplate();
		stringTemplate.setConnectionFactory(connectionFactory);
		stringTemplate.afterPropertiesSet();
		// 构建RedisTemplate
		RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
		template.setConnectionFactory(connectionFactory);
		template.setKeySerializer(keySerializer);
		template.setHashKeySerializer(keySerializer);
		template.setValueSerializer(valueSerializer);
		template.setHashValueSerializer(valueSerializer);
		template.afterPropertiesSet();
		
		// 开始初始化相关组件 
		if(this.isInit == false) {
			this.stringRedisTemplate = stringTemplate;
			this.objectRedisTemplate = template;
			this.isInit = true;
		}
	}
	
	
	/**
	 * 获取Value，如无返空 
	 */
	@Override
	public String get(String key) {
		return stringRedisTemplate.opsForValue().get(key);
	}

	/**
	 * 写入Value，并设定存活时间 (单位: 秒) 
	 */
	@Override
	public void set(String key, String value, long timeout) {
		if(timeout == 0 || timeout <= SaTokenDao.NOT_VALUE_EXPIRE)  {
			return;
		}
		// 判断是否为永不过期 
		if(timeout == SaTokenDao.NEVER_EXPIRE) {
			stringRedisTemplate.opsForValue().set(key, value);
		} else {
			stringRedisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
		}
	}

	/**
	 * 修修改指定key-value键值对 (过期时间不变) 
	 */
	@Override
	public void update(String key, String value) {
		long expire = getTimeout(key);
		// -2 = 无此键 
		if(expire == SaTokenDao.NOT_VALUE_EXPIRE) {
			return;
		}
		this.set(key, value, expire);
	}
	
	/**
	 * 删除Value 
	 */
	@Override
	public void delete(String key) {
		stringRedisTemplate.delete(key);
	}

	/**
	 * 获取Value的剩余存活时间 (单位: 秒) 
	 */
	@Override
	public long getTimeout(String key) {
		return stringRedisTemplate.getExpire(key);
	}

	/**
	 * 修改Value的剩余存活时间 (单位: 秒) 
	 */
	@Override
	public void updateTimeout(String key, long timeout) {
		// 判断是否想要设置为永久
		if(timeout == SaTokenDao.NEVER_EXPIRE) {
			long expire = getTimeout(key);
			if(expire == SaTokenDao.NEVER_EXPIRE) {
				// 如果其已经被设置为永久，则不作任何处理 
			} else {
				// 如果尚未被设置为永久，那么再次set一次
				this.set(key, this.get(key), timeout);
			}
			return;
		}
		stringRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
	}
	
	

	/**
	 * 获取Object，如无返空 
	 */
	@Override
	public Object getObject(String key) {
		return objectRedisTemplate.opsForValue().get(key);
	}

	/**
	 * 写入Object，并设定存活时间 (单位: 秒) 
	 */
	@Override
	public void setObject(String key, Object object, long timeout) {
		if(timeout == 0 || timeout <= SaTokenDao.NOT_VALUE_EXPIRE)  {
			return;
		}
		// 判断是否为永不过期 
		if(timeout == SaTokenDao.NEVER_EXPIRE) {
			objectRedisTemplate.opsForValue().set(key, object);
		} else {
			objectRedisTemplate.opsForValue().set(key, object, timeout, TimeUnit.SECONDS);
		}
	}

	/**
	 * 更新Object (过期时间不变) 
	 */
	@Override
	public void updateObject(String key, Object object) {
		long expire = getObjectTimeout(key);
		// -2 = 无此键 
		if(expire == SaTokenDao.NOT_VALUE_EXPIRE) {
			return;
		}
		this.setObject(key, object, expire);
	}

	/**
	 * 删除Object 
	 */
	@Override
	public void deleteObject(String key) {
		objectRedisTemplate.delete(key);
	}

	/**
	 * 获取Object的剩余存活时间 (单位: 秒)
	 */
	@Override
	public long getObjectTimeout(String key) {
		return objectRedisTemplate.getExpire(key);
	}

	/**
	 * 修改Object的剩余存活时间 (单位: 秒)
	 */
	@Override
	public void updateObjectTimeout(String key, long timeout) {
		// 判断是否想要设置为永久
		if(timeout == SaTokenDao.NEVER_EXPIRE) {
			long expire = getObjectTimeout(key);
			if(expire == SaTokenDao.NEVER_EXPIRE) {
				// 如果其已经被设置为永久，则不作任何处理 
			} else {
				// 如果尚未被设置为永久，那么再次set一次
				this.setObject(key, this.getObject(key), timeout);
			}
			return;
		}
		objectRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
	}

	
	/**
	 * 搜索数据 
	 */
	@Override
	public List<String> searchData(String prefix, String keyword, int start, int size) {
		Set<String> keys = stringRedisTemplate.keys(prefix + "*" + keyword + "*");
		List<String> list = new ArrayList<String>(keys);
		return SaFoxUtil.searchList(list, start, size);
	}
	
}
