package br.jus.trf2.assijus;

import com.crivano.swaggerservlet.IMemCache;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.util.SafeEncoder;

public class MemCacheRedis implements IMemCache {
	private static JedisPool poolMaster;
	private static JedisPool poolSlave;

	static {
		redisConfig();
	}

	private static void redisConfig() {
		String masterhost = getMasterHost();
		int masterport = getMasterPort();
		String slavehost = getSlaveHost();
		int slaveport = getSlavePort();
		String password = getPassword();
		int database = getDatabase();

		poolMaster = new JedisPool(new JedisPoolConfig(), masterhost, masterport, Protocol.DEFAULT_TIMEOUT, password,
				database);

		if (slavehost != null)
			poolSlave = new JedisPool(new JedisPoolConfig(), slavehost, slaveport, Protocol.DEFAULT_TIMEOUT, password,
					database);
		else
			poolSlave = poolMaster;
	}

	public static int getDatabase() {
		return Integer.parseInt(AssijusServlet.getProp("redis.database"));
	}

	public static String getPassword() {
		return AssijusServlet.getProp("redis.password");
	}

	public static int getSlavePort() {
		return Integer.parseInt(AssijusServlet.getProp("redis.slave.port"));
	}

	public static String getSlaveHost() {
		return AssijusServlet.getProp("redis.slave.host");
	}

	public static String getMasterHost() {
		return AssijusServlet.getProp("redis.master.host");
	}

	public static int getMasterPort() {
		return Integer.parseInt(AssijusServlet.getProp("redis.master.port"));
	}

	@Override
	public void store(String sha1, byte[] ba) {
		try (Jedis jedis = poolMaster.getResource()) {
			jedis.set(SafeEncoder.encode(sha1), ba);
		}
	}

	@Override
	public byte[] retrieve(String sha1) {
		try (Jedis jedis = poolSlave.getResource()) {
			byte[] ba = jedis.get(SafeEncoder.encode(sha1));
			return ba;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public byte[] remove(String sha1) {
		try (Jedis jedis = poolMaster.getResource()) {
			byte[] key = SafeEncoder.encode(sha1);
			byte[] ba = jedis.get(key);
			jedis.del(key);
			return ba;
		} catch (Exception e) {
			return null;
		}
	}

}
