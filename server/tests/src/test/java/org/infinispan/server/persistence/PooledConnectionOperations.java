package org.infinispan.server.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.server.test.core.category.Persistence;
import org.infinispan.server.test.core.persistence.Database;
import org.infinispan.server.test.junit4.InfinispanServerRule;
import org.infinispan.server.test.junit4.InfinispanServerTestMethodRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Gustavo Lira &lt;glira@redhat.com&gt;
 * @since 10.0
 **/
@Category(Persistence.class)
@RunWith(Parameterized.class)
public class PooledConnectionOperations {

   @ClassRule
   public static InfinispanServerRule SERVERS = PersistenceIT.SERVERS;

   @Rule
   public InfinispanServerTestMethodRule SERVER_TEST = new InfinispanServerTestMethodRule(SERVERS);

   private final Database database;

   @Parameterized.Parameters(name = "{0}")
   public static Collection<Object[]> data() {
      String[] databaseTypes = PersistenceIT.DATABASE_LISTENER.getDatabaseTypes();
      List<Object[]> params = new ArrayList<>(databaseTypes.length);
      for (String databaseType : databaseTypes) {
         params.add(new Object[]{databaseType});
      }
      return params;
   }

   public PooledConnectionOperations(String databaseType) {
      this.database = PersistenceIT.DATABASE_LISTENER.getDatabase(databaseType);
   }

   private org.infinispan.configuration.cache.ConfigurationBuilder createConfigurationBuilder() {

      org.infinispan.configuration.cache.ConfigurationBuilder builder = new org.infinispan.configuration.cache.ConfigurationBuilder();
      builder.clustering().cacheMode(CacheMode.DIST_SYNC);
      builder.persistence().addStore(JdbcStringBasedStoreConfigurationBuilder.class)
            .fetchPersistentState(false)
            .ignoreModifications(false)
            .purgeOnStartup(false)
            .shared(false)
            .table()
            .dropOnExit(true)
            .createOnStart(true)
            .tableNamePrefix("TBL")
            .idColumnName("ID").idColumnType(database.getIdColumType())
            .dataColumnName("DATA").dataColumnType(database.getDataColumnType())
            .timestampColumnName("TS").timestampColumnType(database.getTimeStampColumnType())
            .segmentColumnName("S").segmentColumnType(database.getSegmentColumnType())
            .connectionPool()
            .connectionUrl(database.jdbcUrl())
            .username(database.username())
            .password(database.password())
            .driverClass(database.driverClassName());
      return builder;
   }

   @Test
   public void testTwoCachesSameCacheStore() {
      RemoteCache<String, String> cache1 = SERVER_TEST.hotrod().withServerConfiguration(createConfigurationBuilder()).withQualifier("1").create();
      RemoteCache<String, String> cache2 = SERVER_TEST.hotrod().withServerConfiguration(createConfigurationBuilder()).withQualifier("2").create();
      cache1.put("k1", "v1");
      String firstK1 = cache1.get("k1");
      assertEquals("v1", firstK1);
      assertNull(cache2.get("k1"));

      cache2.put("k2", "v2");
      assertEquals("v2", cache2.get("k2"));
      assertNull(cache1.get("k2"));

      assertCleanCacheAndStore(cache1);
      assertCleanCacheAndStore(cache2);
   }

   @Test
   public void testPutGetRemove() {
      RemoteCache<String, String> cache = SERVER_TEST.hotrod().withServerConfiguration(createConfigurationBuilder()).create();
      cache.put("k1", "v1");
      cache.put("k2", "v2");

      assertNotNull(cache.get("k1"));
      assertNotNull(cache.get("k2"));

      cache.stop();
      cache.start();

      assertNotNull(cache.get("k1"));
      assertNotNull(cache.get("k2"));
      assertEquals("v1", cache.get("k1"));
      assertEquals("v2", cache.get("k2"));
      cache.remove("k1");
      assertNull(cache.get("k1"));
   }

   protected void assertCleanCacheAndStore(RemoteCache cache) {
      cache.clear();
      assertEquals(0, cache.size());
   }

}
